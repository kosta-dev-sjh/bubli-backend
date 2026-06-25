package com.bubli.resource.dto;

import com.bubli.agent.type.AgentRequestStatus;
import com.bubli.resource.entity.Document;
import com.bubli.resource.type.DocumentStatus;

import java.util.UUID;

public record DocumentUploadResponse(
        UUID documentId,
        UUID requestId,
        String fileName,
        DocumentStatus documentStatus,
        AgentRequestStatus requestStatus
) {

    public static DocumentUploadResponse of(Document document, UUID requestId, AgentRequestStatus requestStatus) {
        return new DocumentUploadResponse(
                document.getId(),
                requestId,
                document.getFileName(),
                document.getStatus(),
                requestStatus
        );
    }
}
