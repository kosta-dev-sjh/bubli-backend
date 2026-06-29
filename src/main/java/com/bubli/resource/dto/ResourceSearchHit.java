package com.bubli.resource.dto;

import java.util.UUID;

public record ResourceSearchHit(
        UUID embeddingId,
        UUID resourceId,
        int chunkIndex,
        String chunkText,
        Integer pageNumber,
        String chunkMetadata,
        double similarityScore
) {
}
