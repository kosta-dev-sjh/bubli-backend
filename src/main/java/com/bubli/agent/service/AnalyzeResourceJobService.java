package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.entity.AiDocument;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.type.DocumentType;
import com.bubli.storage.service.FileStorage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeResourceJobService {

    private static final int SUMMARY_PREVIEW_LIMIT = 500;

    private final AgentJobRepository agentJobRepository;
    private final ResourceRepository resourceRepository;
    private final ResourceFileRepository resourceFileRepository;
    private final ResourceSummaryRepository resourceSummaryRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final FileStorage fileStorage;

    @Transactional
    public AgentJob process(UUID jobId) {
        AgentJob job = agentJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Agent job not found."));

        if (job.getJobType() != AgentJobType.ANALYZE_RESOURCE) {
            throw new IllegalArgumentException("Only ANALYZE_RESOURCE jobs can be processed.");
        }
        if (job.getStatus() != AgentJobStatus.PENDING) {
            return job;
        }

        Resource resource = null;
        try {
            job.start();

            resource = resourceRepository.findById(job.getResourceId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
            resource.startAnalysis();

            ResourceFile resourceFile = resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resource.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Resource file not found."));

            ExtractedText extracted = extract(resourceFile);
            if (extracted.text().isBlank()) {
                throw new IllegalArgumentException("Extracted text is empty.");
            }

            resourceSummaryRepository.save(ResourceSummary.analyzed(
                    resource.getId(),
                    job.getId(),
                    summaryJson(resourceFile, extracted)
            ));

            UUID resourceId = resource.getId();
            UUID roomId = resource.getRoomId();
            aiDocumentRepository.findByResourceId(resourceId)
                    .orElseGet(() -> aiDocumentRepository.save(AiDocument.analyzed(
                            resourceId,
                            roomId,
                            detectDocumentType(resourceFile, extracted.text()),
                            new BigDecimal("0.5000")
                    )));

            resource.markAnalyzed();
            job.succeed();
            return job;
        } catch (RuntimeException e) {
            if (resource != null) {
                resource.markAnalysisFailed();
            }
            job.fail("RESOURCE_ANALYSIS_FAILED", safeMessage(e));
            return job;
        }
    }

    @Transactional
    public int processPendingBatch() {
        List<AgentJob> jobs = agentJobRepository.findTop20ByJobTypeAndStatusOrderByCreatedAtAsc(
                AgentJobType.ANALYZE_RESOURCE,
                AgentJobStatus.PENDING
        );
        jobs.forEach(job -> process(job.getId()));
        return jobs.size();
    }

    private ExtractedText extract(ResourceFile resourceFile) {
        try (InputStream inputStream = fileStorage.open(resourceFile.getStorageKey())) {
            if (resourceFile.getMimeType().startsWith("application/pdf")) {
                return extractPdf(inputStream);
            }
            if (resourceFile.getMimeType().startsWith("text/plain")) {
                String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                return new ExtractedText(text, 1);
            }
            throw new IllegalArgumentException("Unsupported resource file mime type.");
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to extract resource text.", e);
        }
    }

    private ExtractedText extractPdf(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return new ExtractedText(stripper.getText(document), document.getNumberOfPages());
        }
    }

    private Map<String, Object> summaryJson(ResourceFile resourceFile, ExtractedText extracted) {
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
        if (source.contains("contract") || source.contains("계약")) {
            return DocumentType.CONTRACT;
        }
        if (source.contains("requirement") || source.contains("요구사항") || source.contains("요구")) {
            return DocumentType.REQUIREMENT;
        }
        if (source.contains("meeting") || source.contains("회의")) {
            return DocumentType.MEETING_NOTE;
        }
        return DocumentType.REFERENCE;
    }

    private String safeMessage(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private record ExtractedText(String text, int pageCount) {
    }
}
