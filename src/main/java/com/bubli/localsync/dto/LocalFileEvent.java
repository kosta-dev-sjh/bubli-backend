package com.bubli.localsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LocalFileEvent(
        @NotNull String eventType,
        String fileName,
        Long fileSizeBytes,
        @NotBlank @Size(max = 120) String localEventId,
        String mimeType,
        java.util.UUID resourceId
) {}
