package com.bubli.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record ResourceDownloadUrlResponse(
		UUID resourceId,
		UUID fileId,
		Integer versionNo,
		String downloadUrl,
		Instant expiresAt,
		String originalName,
		String mimeType,
		Long sizeBytes
) {
	public static ResourceDownloadUrlResponse from(ResourceDownloadUrlResult result) {
		return new ResourceDownloadUrlResponse(
				result.resourceId(),
				result.fileId(),
				result.versionNo(),
				result.downloadUrl(),
				result.expiresAt(),
				result.originalName(),
				result.mimeType(),
				result.sizeBytes()
		);
	}
}
