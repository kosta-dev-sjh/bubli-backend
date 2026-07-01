package com.bubli.localsync.dto;

import java.util.UUID;

public record LocalFileSyncResult(
        String eventType,
        UUID resourceId,
        String status
) {}
