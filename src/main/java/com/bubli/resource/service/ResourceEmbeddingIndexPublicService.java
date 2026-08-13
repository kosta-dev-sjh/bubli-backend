package com.bubli.resource.service;

import com.bubli.global.ai.AiModelGateway;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex;
import com.bubli.resource.dto.PreparedResourceEmbeddingIndex.PreparedEmbedding;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ResourceEmbeddingIndexPublicService {

    private static final int EMBEDDING_BATCH_SIZE = 32;

    private final ResourceEmbeddingRepository resourceEmbeddingRepository;
    private final TextChunker textChunker;
    private final AiModelGateway aiModelGateway;
    private final EmbeddingVectorFormatter embeddingVectorFormatter;
    private final ObjectMapper objectMapper;

    public PreparedResourceEmbeddingIndex prepare(
            Resource resource,
            ResourceFile resourceFile,
            List<TextChunker.TextPage> pages
    ) {
        return prepareChunks(
                resource,
                resourceFile.getOriginalName(),
                resourceFile.getMimeType(),
                pages
        );
    }

    public PreparedResourceEmbeddingIndex prepareExtractedText(
            Resource resource,
            String originalName,
            String mimeType,
            List<TextChunker.TextPage> pages
    ) {
        return prepareChunks(resource, originalName, mimeType, pages);
    }

    /**
     * Replaces a resource index using vectors that were already computed. This method performs no remote AI calls.
     * The caller is responsible for invoking it inside the transaction that commits the analysis artifacts.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public IndexResult replace(PreparedResourceEmbeddingIndex preparedIndex) {
        if (!preparedIndex.indexed()) {
            return IndexResult.skipped();
        }
        resourceEmbeddingRepository.deleteAllByResourceId(preparedIndex.resourceId());
        preparedIndex.embeddings().forEach(this::insertEmbedding);
        return IndexResult.indexed(preparedIndex.embeddings().size());
    }

    private PreparedResourceEmbeddingIndex prepareChunks(
            Resource resource,
            String originalName,
            String mimeType,
            List<TextChunker.TextPage> pages
    ) {
        if (!aiModelGateway.isEmbeddingAvailable()) {
            return PreparedResourceEmbeddingIndex.skipped(resource.getId());
        }
        List<TextChunker.TextChunk> chunks = textChunker.splitPages(pages);
        List<float[]> vectors = embedChunks(chunks);
        List<PreparedEmbedding> embeddings = IntStream.range(0, chunks.size())
                .mapToObj(index -> prepareEmbedding(
                        resource,
                        originalName,
                        mimeType,
                        chunks.get(index),
                        vectors.get(index)
                ))
                .toList();
        return PreparedResourceEmbeddingIndex.indexed(resource.getId(), embeddings);
    }

    private PreparedEmbedding prepareEmbedding(
            Resource resource,
            String originalName,
            String mimeType,
            TextChunker.TextChunk chunk,
            float[] vector
    ) {
        String chunkLanguage = ResourceDocumentLanguageDetector.detect(chunk.text());
        return new PreparedEmbedding(
                resource.getId(),
                resource.getOwnerId(),
                resource.getRoomId(),
                resource.getVisibility(),
                chunk.index(),
                chunk.text(),
                embeddingVectorFormatter.toVectorLiteral(vector),
                metadataJson(metadata(originalName, mimeType, chunk, chunkLanguage))
        );
    }

    private List<float[]> embedChunks(List<TextChunker.TextChunk> chunks) {
        if (chunks.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<float[]> embeddings = new java.util.ArrayList<>(chunks.size());
        for (int start = 0; start < chunks.size(); start += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(start + EMBEDDING_BATCH_SIZE, chunks.size());
            List<String> texts = chunks.subList(start, end).stream()
                    .map(TextChunker.TextChunk::text)
                    .toList();
            embeddings.addAll(aiModelGateway.embedAll("resource-index-embedding", texts));
        }
        return List.copyOf(embeddings);
    }

    private void insertEmbedding(PreparedEmbedding embedding) {
        resourceEmbeddingRepository.insertEmbedding(
                UUID.randomUUID(),
                embedding.resourceId(),
                embedding.ownerId(),
                embedding.roomId(),
                embedding.visibility().name(),
                embedding.chunkIndex(),
                embedding.chunkText(),
                embedding.embedding(),
                embedding.chunkMetadataJson()
        );
    }

    private Map<String, Object> metadata(
            String originalName,
            String mimeType,
            TextChunker.TextChunk chunk,
            String chunkLanguage
    ) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalName", originalName);
        metadata.put("mimeType", mimeType);
        metadata.put("pageNumber", chunk.pageNumber());
        metadata.put("startOffset", chunk.startOffset());
        metadata.put("endOffset", chunk.endOffset());
        metadata.put("startLine", chunk.startLine());
        metadata.put("endLine", chunk.endLine());
        metadata.put("characterCount", chunk.text().length());
        metadata.put("documentLanguage", chunkLanguage);
        return metadata;
    }

    private String metadataJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize resource embedding metadata.", exception);
        }
    }

    public record IndexResult(
            boolean indexed,
            int chunkCount
    ) {

        static IndexResult indexed(int chunkCount) {
            return new IndexResult(true, chunkCount);
        }

        static IndexResult skipped() {
            return new IndexResult(false, 0);
        }
    }
}
