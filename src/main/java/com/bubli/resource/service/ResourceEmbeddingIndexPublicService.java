package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResourceEmbeddingIndexPublicService {

    private final ResourceEmbeddingRepository resourceEmbeddingRepository;
    private final TextChunker textChunker;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final EmbeddingVectorFormatter embeddingVectorFormatter;

    public IndexResult index(Resource resource, ResourceFile resourceFile, String text) {
        return index(resource, resourceFile, List.of(new TextChunker.TextPage(null, text)));
    }

    public IndexResult index(Resource resource, ResourceFile resourceFile, List<TextChunker.TextPage> pages) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return IndexResult.skipped();
        }

        List<TextChunker.TextChunk> chunks = textChunker.splitPages(pages);
        resourceEmbeddingRepository.deleteAllByResourceId(resource.getId());

        List<ResourceEmbedding> embeddings = chunks.stream()
                .map(chunk -> toEmbedding(resource, resourceFile, chunk, embeddingModel.embed(chunk.text())))
                .toList();
        resourceEmbeddingRepository.saveAll(embeddings);
        return IndexResult.indexed(embeddings.size());
    }

    private ResourceEmbedding toEmbedding(
            Resource resource,
            ResourceFile resourceFile,
            TextChunker.TextChunk chunk,
            float[] embedding
    ) {
        return ResourceEmbedding.create(
                resource.getId(),
                resource.getOwnerId(),
                resource.getRoomId(),
                resource.getVisibility(),
                chunk.index(),
                chunk.text(),
                embeddingVectorFormatter.toVectorLiteral(embedding),
                metadata(resourceFile, chunk)
        );
    }

    private Map<String, Object> metadata(ResourceFile resourceFile, TextChunker.TextChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalName", resourceFile.getOriginalName());
        metadata.put("mimeType", resourceFile.getMimeType());
        metadata.put("pageNumber", chunk.pageNumber());
        metadata.put("startOffset", chunk.startOffset());
        metadata.put("endOffset", chunk.endOffset());
        metadata.put("characterCount", chunk.text().length());
        return metadata;
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
