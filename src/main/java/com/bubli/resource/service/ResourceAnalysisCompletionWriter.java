package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex;
import com.bubli.resource.entity.AiDocument;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceAnalysisCompletionWriter {

    private final ResourceRepository resourceRepository;
    private final ResourceSummaryRepository resourceSummaryRepository;
    private final AiDocumentRepository aiDocumentRepository;
    private final ResourceEmbeddingIndexPublicService resourceEmbeddingIndexService;
    private final ResourceRelationIndexPublicService resourceRelationIndexService;

    @Transactional
    public void complete(
            ResourceAnalysisSource source,
            UUID jobId,
            Map<String, Object> summaryJson,
            boolean llmAnalyzed,
            PreparedResourceEmbeddingIndex preparedIndex
    ) {
        if (!source.resourceId().equals(preparedIndex.resourceId())) {
            throw new IllegalArgumentException("Prepared index belongs to a different resource.");
        }

        Resource resource = resourceRepository.lockById(source.resourceId())
                .orElseThrow(() -> new IllegalArgumentException("Resource not found."));
        resourceSummaryRepository.save(ResourceSummary.analyzed(resource.getId(), jobId, summaryJson));

        BigDecimal detectedConfidence = llmAnalyzed
                ? new BigDecimal("0.8000")
                : new BigDecimal("0.5000");
        aiDocumentRepository.findByResourceId(resource.getId())
                .ifPresentOrElse(
                        aiDocument -> aiDocument.markAnalyzed(source.documentType(), detectedConfidence),
                        () -> aiDocumentRepository.save(AiDocument.analyzed(
                                resource.getId(),
                                resource.getRoomId(),
                                source.documentType(),
                                detectedConfidence
                        ))
                );

        ResourceEmbeddingIndexPublicService.IndexResult indexResult =
                resourceEmbeddingIndexService.replace(preparedIndex);
        if (indexResult.indexed()) {
            resourceRelationIndexService.rebuildRelations(resource);
        }
        resource.markAnalyzed();
    }
}
