package com.bubli.agent.dto;

import com.bubli.agent.entity.GeneratedDocument;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record GeneratedDocumentResponse(
        UUID id,
        UUID userId,
        UUID roomId,
        UUID suggestionId,
        UUID resourceId,
        String title,
        String documentType,
        String contentMarkdown,
        Map<String, Object> metadataJson,
        Instant createdAt,
        Instant updatedAt
) {

    public static GeneratedDocumentResponse from(GeneratedDocument document) {
        return new GeneratedDocumentResponse(
                document.getId(),
                document.getUserId(),
                document.getRoomId(),
                document.getSuggestionId(),
                document.getResourceId(),
                document.getTitle(),
                document.getDocumentType(),
                document.getContentMarkdown(),
                document.getMetadataJson(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
