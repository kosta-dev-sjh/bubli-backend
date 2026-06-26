package com.bubli.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record ResourceDownloadUrlResult(
		UUID resourceId,
		UUID fileId,
		Integer versionNo,
		String downloadUrl,
		Instant expiresAt,
		String originalName,
		String mimeType,
		Long sizeBytes
) {
}
