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
        String downloadUrl,
        String exportUrl,
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
                exportPath(document.getId()),
                exportPath(document.getId()),
                document.getMetadataJson(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private static String exportPath(UUID documentId) {
        return "/api/generated-documents/%s/export".formatted(documentId);
    }
}
