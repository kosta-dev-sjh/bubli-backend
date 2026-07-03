package com.bubli.localsync.dto;

import java.util.UUID;

public record LocalFileSyncResult(
        String eventType,
        String localEventId,
        UUID resourceId,
        String status
) {}
