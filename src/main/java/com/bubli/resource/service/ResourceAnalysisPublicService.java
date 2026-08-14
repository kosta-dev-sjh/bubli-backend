package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceAnalysisArtifacts;
import com.bubli.resource.dto.ResourceAnalysisPage;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.dto.ResourceAnalysisTarget;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex;
import com.bubli.resource.entity.AiDocument;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceExtractedText;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceExtractedTextRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.type.DocumentType;
import com.bubli.storage.service.StoragePublicService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceAnalysisPublicService {

    private static final int SUMMARY_PREVIEW_LIMIT = 500;

    private final ResourceRepository resourceRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final ResourceExtractedTextRepository resourceExtractedTextRepository;
    private final ResourceSummaryRepository resourceSummaryRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final ResourceEmbeddingIndexPublicService resourceEmbeddingIndexService;
    private final ResourceAnalysisCompletionWriter resourceAnalysisCompletionWriter;
    private final ResourceAnalysisStateWriter resourceAnalysisStateWriter;
    private final StoragePublicService storageService;

    public ResourceAnalysisTarget startAnalysis(UUID resourceId) {
        return resourceAnalysisStateWriter.start(resourceId);
    }

    @Transactional(readOnly = true)
    public ResourceAnalysisArtifacts findArtifacts(UUID resourceId) {
        UUID resourceSummaryId = resourceSummaryRepository.findTopByResourceIdOrderByCreatedAtDesc(resourceId)
                .map(ResourceSummary::getId)
                .orElse(null);
        UUID aiDocumentId = aiDocumentRepository.findByResourceId(resourceId)
                .map(AiDocument::getId)
                .orElse(null);
        return new ResourceAnalysisArtifacts(resourceSummaryId, aiDocumentId);
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> findReusableAnalysisForJob(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));
        Optional<ResourceFile> resourceFile = resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resource.getId());
        if (resourceFile.isEmpty()) {
            return Optional.empty();
        }
        if (resourceFile.get().getChecksum() == null || resourceFile.get().getChecksum().isBlank()) {
            return Optional.empty();
        }
        return resourceSummaryRepository.findReusableAnalysisSummaries(
                        resource.getId(),
                        resourceFile.get().getChecksum(),
                        resource.getRoomId(),
                        resource.getVisibility(),
                        PageRequest.of(0, 5)
                )
                .stream()
                .map(ResourceSummary::getSummaryJson)
                .map(this::cachedAnalysis)
                .flatMap(Optional::stream)
                .findFirst();
    }

    public void analyzeResourceForJob(UUID resourceId, UUID jobId) {
        ResourceAnalysisSource source = loadAnalysisSourceForJob(resourceId);
        completeAnalysisForJob(source, jobId, null);
    }

    public ResourceAnalysisSource loadAnalysisSourceForJob(UUID resourceId) {
        Resource resource = null;
        try {
            resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
            resourceAnalysisStateWriter.start(resourceId);

            Optional<ResourceAnalysisSource> extractedTextSource = loadExtractedTextSource(resource);
            if (extractedTextSource.isPresent()) {
                return extractedTextSource.get();
            }

            ResourceFile resourceFile = resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resource.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource file not found."));
            ExtractedDocument extracted = extract(resourceFile);
            if (extracted.text().isBlank()) {
                throw new IllegalArgumentException("Extracted text is empty.");
            }

            DocumentType documentType = detectDocumentType(resourceFile, extracted.text());
            return new ResourceAnalysisSource(
                    resource.getId(),
                    resource.getRoomId(),
                    resourceFile.getOriginalName(),
                    resourceFile.getMimeType(),
                    documentType,
                    extracted.pages().stream()
                            .map(page -> new ResourceAnalysisPage(page.pageNumber(), page.text()))
                            .toList(),
                    extracted.text(),
                    extracted.pageCount(),
                    extracted.text().length()
            );
        } catch (RuntimeException e) {
            if (resource != null) {
                try {
                    resourceAnalysisStateWriter.markFailed(resource.getId());
                } catch (RuntimeException failure) {
                    e.addSuppressed(failure);
                }
            }
            throw e;
        }
    }

    public void completeAnalysisForJob(ResourceAnalysisSource source, UUID jobId, Map<String, Object> aiAnalysisJson) {
        PreparedResourceEmbeddingIndex preparedIndex = prepareEmbeddingIndex(source);
        completePreparedAnalysisForJob(source, jobId, aiAnalysisJson, preparedIndex);
    }

    public PreparedResourceEmbeddingIndex prepareEmbeddingIndex(ResourceAnalysisSource source) {
        Resource resource = resourceRepository.findById(source.resourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
        Optional<ResourceFile> resourceFile = resourceFileRepository
                .findTopByResourceIdOrderByCreatedAtDesc(resource.getId());
        List<TextChunker.TextPage> pages = pages(source);
        if (resourceFile.isPresent()) {
            return resourceEmbeddingIndexService.prepare(resource, resourceFile.get(), pages);
        }
        return resourceEmbeddingIndexService.prepareExtractedText(
                resource,
                source.originalName(),
                source.mimeType(),
                pages
        );
    }

    public void completePreparedAnalysisForJob(
            ResourceAnalysisSource source,
            UUID jobId,
            Map<String, Object> aiAnalysisJson,
            PreparedResourceEmbeddingIndex preparedIndex
    ) {
        ExtractedDocument extracted = new ExtractedDocument(pages(source));
        resourceAnalysisCompletionWriter.complete(
                source,
                jobId,
                summaryJson(source.originalName(), source.mimeType(), extracted, source.documentType(), aiAnalysisJson),
                aiAnalysisJson != null,
                preparedIndex
        );
    }

    public void markAnalysisFailed(UUID resourceId) {
        resourceAnalysisStateWriter.markFailed(resourceId);
    }

    private List<TextChunker.TextPage> pages(ResourceAnalysisSource source) {
        return source.pages().stream()
                .map(page -> new TextChunker.TextPage(page.pageNumber(), page.text()))
                .toList();
    }

    private ExtractedDocument extract(ResourceFile resourceFile) {
        try (InputStream inputStream = storageService.open(resourceFile.getStorageKey())) {
            if (isPdf(resourceFile)) {
                return extractPdf(inputStream);
            }
            if (isText(resourceFile) || isMarkdown(resourceFile)) {
                return extractUtf8Text(inputStream);
            }
            if (isDocx(resourceFile)) {
                return extractDocx(inputStream);
            }
            throw new BusinessException(ErrorCode.RESOURCE_415_001);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private ExtractedDocument extractUtf8Text(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return new ExtractedDocument(List.of(new TextChunker.TextPage(null, text)));
    }

    private ExtractedDocument extractPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            List<TextChunker.TextPage> pages = new ArrayList<>();
            int pageCount = document.getNumberOfPages();
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pages.add(new TextChunker.TextPage(pageNumber, stripper.getText(document)));
            }
            return new ExtractedDocument(pages);
        }
    }

    private ExtractedDocument extractDocx(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                appendLine(text, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                appendTable(text, table);
            }
            return new ExtractedDocument(List.of(new TextChunker.TextPage(null, text.toString())));
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.RESOURCE_415_001);
        }
    }

    private void appendTable(StringBuilder text, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = row.getTableCells().stream()
                    .map(XWPFTableCell::getText)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
            appendLine(text, String.join(" | ", cells));
        }
    }

    private void appendLine(StringBuilder text, String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        if (text.length() > 0) {
            text.append('\n');
        }
        text.append(line.trim());
    }

    private boolean isPdf(ResourceFile resourceFile) {
        return mimeType(resourceFile).startsWith("application/pdf")
                || originalName(resourceFile).endsWith(".pdf");
    }

    private boolean isText(ResourceFile resourceFile) {
        return mimeType(resourceFile).startsWith("text/plain")
                || originalName(resourceFile).endsWith(".txt");
    }

    private boolean isMarkdown(ResourceFile resourceFile) {
        String originalName = originalName(resourceFile);
        return mimeType(resourceFile).startsWith("text/markdown")
                || originalName.endsWith(".md")
                || originalName.endsWith(".markdown");
    }

    private boolean isDocx(ResourceFile resourceFile) {
        return mimeType(resourceFile).startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || originalName(resourceFile).endsWith(".docx");
    }

    private String mimeType(ResourceFile resourceFile) {
        return resourceFile.getMimeType() == null ? "" : resourceFile.getMimeType().toLowerCase(Locale.ROOT);
    }

    private String originalName(ResourceFile resourceFile) {
        return resourceFile.getOriginalName() == null ? "" : resourceFile.getOriginalName().toLowerCase(Locale.ROOT);
    }

    private Optional<ResourceAnalysisSource> loadExtractedTextSource(Resource resource) {
        return resourceExtractedTextRepository.findFirstByResourceIdOrderByUpdatedAtDescIdDesc(resource.getId())
                .map(extractedText -> extractedTextSource(resource, extractedText));
    }

    private ResourceAnalysisSource extractedTextSource(Resource resource, ResourceExtractedText extractedText) {
        String text = extractedText.getCombinedText();
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Extracted text is empty.");
        }
        String originalName = resource.getTitle();
        String mimeType = "text/plain";
        DocumentType documentType = detectDocumentType(originalName, text);
        return new ResourceAnalysisSource(
                resource.getId(),
                resource.getRoomId(),
                originalName,
                mimeType,
                documentType,
                List.of(new ResourceAnalysisPage(null, text)),
                text,
                1,
                text.length()
        );
    }

    private Map<String, Object> summaryJson(
            String originalName,
            String mimeType,
            ExtractedDocument extracted,
            DocumentType documentType,
            Map<String, Object> aiAnalysisJson
    ) {
        String normalized = extracted.text().replaceAll("\\s+", " ").trim();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summary", aiSummary(aiAnalysisJson, normalized));
        summary.put("source", aiAnalysisJson == null ? "LOCAL_EXTRACTOR" : "LLM_ANALYZER");
        summary.put("documentType", documentType.name());
        summary.put("originalName", originalName);
        summary.put("mimeType", mimeType);
        summary.put("pageCount", extracted.pageCount());
        summary.put("characterCount", extracted.text().length());
        summary.put("lineCount", extracted.text().lines().count());
        if (aiAnalysisJson != null) {
            summary.put("analysis", aiAnalysisJson);
        }
        return summary;
    }

    private String aiSummary(Map<String, Object> aiAnalysisJson, String normalizedText) {
        if (aiAnalysisJson == null) {
            return preview(normalizedText);
        }
        Object summary = aiAnalysisJson.get("summary");
        if (summary == null || summary.toString().isBlank()) {
            return preview(normalizedText);
        }
        return summary.toString();
    }

    @SuppressWarnings("unchecked")
    private Optional<Map<String, Object>> cachedAnalysis(Map<String, Object> summaryJson) {
        if (summaryJson == null || !"LLM_ANALYZER".equals(summaryJson.get("source"))) {
            return Optional.empty();
        }
        Object analysis = summaryJson.get("analysis");
        if (!(analysis instanceof Map<?, ?> analysisMap)) {
            return Optional.empty();
        }
        Map<String, Object> cached = new LinkedHashMap<>((Map<String, Object>) analysisMap);
        cached.put("cacheHit", true);
        return Optional.of(cached);
    }

    private String preview(String text) {
        if (text.length() <= SUMMARY_PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, SUMMARY_PREVIEW_LIMIT);
    }

    private DocumentType detectDocumentType(ResourceFile resourceFile, String text) {
        return detectDocumentType(resourceFile.getOriginalName(), text);
    }

    private DocumentType detectDocumentType(String originalName, String text) {
        String source = ((originalName == null ? "" : originalName) + " " + preview(text))
                .toLowerCase(Locale.ROOT);
        if (source.contains("contract") || source.contains("계약") || source.contains("契約")) {
            return DocumentType.CONTRACT;
        }
        if (source.contains("requirement") || source.contains("요구사항") || source.contains("요건")
                || source.contains("要件")) {
            return DocumentType.REQUIREMENT;
        }
        if (source.contains("meeting") || source.contains("회의") || source.contains("議事録")) {
            return DocumentType.MEETING_NOTE;
        }
        return DocumentType.REFERENCE;
    }

    private record ExtractedDocument(List<TextChunker.TextPage> pages) {

        String text() {
            return pages.stream()
                    .map(TextChunker.TextPage::text)
                    .reduce("", (left, right) -> left + "\n" + right);
        }

        int pageCount() {
            return pages.size();
        }
    }
}
