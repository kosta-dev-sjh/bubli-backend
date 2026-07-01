package com.bubli.localsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocalFileEvent(
        @NotNull String eventType,
        String fileName,
        Long fileSizeBytes,
        String mimeType,
        java.util.UUID resourceId
) {}
