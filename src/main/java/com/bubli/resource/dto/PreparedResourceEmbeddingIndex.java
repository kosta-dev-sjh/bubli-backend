package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceVisibility;

import java.util.List;
import java.util.UUID;

public record PreparedResourceEmbeddingIndex(
        UUID resourceId,
        boolean indexed,
        List<PreparedEmbedding> embeddings
) {

    public PreparedResourceEmbeddingIndex {
        if (resourceId == null) {
            throw new IllegalArgumentException("resourceId is required.");
        }
        embeddings = embeddings == null ? List.of() : List.copyOf(embeddings);
        if (!indexed && !embeddings.isEmpty()) {
            throw new IllegalArgumentException("Skipped indexes must not contain embeddings.");
        }
        if (embeddings.stream().anyMatch(embedding -> !resourceId.equals(embedding.resourceId()))) {
            throw new IllegalArgumentException("All embeddings must belong to the prepared resource.");
        }
    }

    public static PreparedResourceEmbeddingIndex indexed(
            UUID resourceId,
            List<PreparedEmbedding> embeddings
    ) {
        return new PreparedResourceEmbeddingIndex(resourceId, true, embeddings);
    }

    public static PreparedResourceEmbeddingIndex skipped(UUID resourceId) {
        return new PreparedResourceEmbeddingIndex(resourceId, false, List.of());
    }

    public int chunkCount() {
        return embeddings.size();
    }

    public record PreparedEmbedding(
            UUID resourceId,
            UUID ownerId,
            UUID roomId,
            ResourceVisibility visibility,
            int chunkIndex,
            String chunkText,
            String embedding,
            String chunkMetadataJson
    ) {
    }
}
