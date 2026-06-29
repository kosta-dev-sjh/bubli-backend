package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceAnalysisArtifacts;
import com.bubli.resource.dto.ResourceAnalysisTarget;
import com.bubli.resource.entity.AiDocument;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.type.DocumentType;
import com.bubli.storage.service.StoragePublicService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceAnalysisPublicService {

    private static final int SUMMARY_PREVIEW_LIMIT = 500;

    private final ResourceRepository resourceRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final ResourceSummaryRepository resourceSummaryRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final ResourceEmbeddingIndexPublicService resourceEmbeddingIndexService;
    private final StoragePublicService storageService;

    @Transactional
    public ResourceAnalysisTarget startAnalysis(UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));

        resource.startAnalysis();
        return new ResourceAnalysisTarget(resource.getId(), resource.getRoomId());
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

    @Transactional
    public void analyzeResourceForJob(UUID resourceId, UUID jobId) {
        Resource resource = null;
        try {
            resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
            resource.startAnalysis();

            ResourceFile resourceFile = resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resource.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource file not found."));
            //파일 추출
            ExtractedDocument extracted = extract(resourceFile);
            if (extracted.text().isBlank()) {
                throw new IllegalArgumentException("Extracted text is empty.");
            }
            //파일+agent job+ summary
            resourceSummaryRepository.save(ResourceSummary.analyzed(
                    resource.getId(),
                    jobId,
                    summaryJson(resourceFile, extracted)
            ));
            //멱등성 보장을 위해 이미있으면 저장 안하고 없으면 생성
            UUID analyzedResourceId = resource.getId();
            UUID roomId = resource.getRoomId();
            aiDocumentRepository.findByResourceId(analyzedResourceId)
                    .orElseGet(() -> aiDocumentRepository.save(AiDocument.analyzed(
                            analyzedResourceId,
                            roomId,
                            detectDocumentType(resourceFile, extracted.text()),
                            new BigDecimal("0.5000")
                    )));
            //임베딩
            resourceEmbeddingIndexService.index(resource, resourceFile, extracted.pages());
            resource.markAnalyzed();
        } catch (RuntimeException e) {
            if (resource != null) {
                resource.markAnalysisFailed();
            }
            throw e;
        }
    }

    private ExtractedDocument extract(ResourceFile resourceFile) {
        //파일 종류별로
        try (InputStream inputStream = storageService.open(resourceFile.getStorageKey())) {
            if (resourceFile.getMimeType().startsWith("application/pdf")) {
                return extractPdf(inputStream);
            }
            if (resourceFile.getMimeType().startsWith("text/plain")) {
                String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return new ExtractedDocument(List.of(new TextChunker.TextPage(null, text)));
            }
            throw new IllegalArgumentException("Unsupported resource file mime type.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to extract resource text.", e);
        }
    }
    //PDF
    private ExtractedDocument extractPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            List<TextChunker.TextPage> pages = new ArrayList<>();
            int pageCount = document.getNumberOfPages();
            //page별로 문서 나눔
            for (int pageNumber = 1; pageNumber <= pageCount; pageNumber++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                pages.add(new TextChunker.TextPage(pageNumber, stripper.getText(document)));
            }
            return new ExtractedDocument(pages);
        }
    }

    private Map<String, Object> summaryJson(ResourceFile resourceFile, ExtractedDocument extracted) {
        String normalized = extracted.text().replaceAll("\\s+", " ").trim();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("summary", preview(normalized));
        summary.put("source", "LOCAL_EXTRACTOR");
        summary.put("originalName", resourceFile.getOriginalName());
        summary.put("mimeType", resourceFile.getMimeType());
        summary.put("pageCount", extracted.pageCount());
        summary.put("characterCount", extracted.text().length());
        summary.put("lineCount", extracted.text().lines().count());
        return summary;
    }

    private String preview(String text) {
        if (text.length() <= SUMMARY_PREVIEW_LIMIT) {
            return text;
        }
        return text.substring(0, SUMMARY_PREVIEW_LIMIT);
    }

    private DocumentType detectDocumentType(ResourceFile resourceFile, String text) {
        String source = (resourceFile.getOriginalName() + " " + preview(text))
                .toLowerCase(Locale.ROOT);
        if (source.contains("contract") || source.contains("怨꾩빟")) {
            return DocumentType.CONTRACT;
        }
        if (source.contains("requirement") || source.contains("?붽뎄?ы빆") || source.contains("?붽뎄")) {
            return DocumentType.REQUIREMENT;
        }
        if (source.contains("meeting") || source.contains("?뚯쓽")) {
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
