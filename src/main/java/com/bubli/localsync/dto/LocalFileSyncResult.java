package com.bubli.localsync.dto;

import java.util.UUID;

public record LocalFileSyncResult(
        String eventType,
        String localEventId,
        UUID resourceId,
        String status,
        String errorCode,
        boolean retryable
) {
    public LocalFileSyncResult(String eventType, String localEventId, UUID resourceId, String status) {
        this(eventType, localEventId, resourceId, status, null,
                "PROCESSING".equals(status) || "FAILED".equals(status));
    }
}
