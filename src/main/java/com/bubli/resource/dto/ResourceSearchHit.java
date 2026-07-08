package com.bubli.resource.dto;

import java.util.UUID;

public record ResourceSearchHit(
        UUID embeddingId,
        UUID resourceId,
        int chunkIndex,
        String chunkText,
        Integer pageNumber,
        Integer startLine,
        Integer endLine,
        Integer startOffset,
        Integer endOffset,
        String originalName,
        String chunkMetadata,
        double similarityScore
) {
}
