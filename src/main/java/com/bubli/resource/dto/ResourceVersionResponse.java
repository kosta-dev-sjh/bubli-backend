package com.bubli.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record ResourceVersionResponse(
		UUID id,
		UUID resourceId,
		Integer versionNo,
		UUID fileId,
		String storageKey,
		String originalName,
		String mimeType,
		Long sizeBytes,
		String checksum,
		UUID createdBy,
		Instant createdAt
) {
	public static ResourceVersionResponse from(ResourceVersionResult result) {
		return new ResourceVersionResponse(
				result.id(),
				result.resourceId(),
				result.versionNo(),
				result.fileId(),
				result.storageKey(),
				result.originalName(),
				result.mimeType(),
				result.sizeBytes(),
				result.checksum(),
				result.createdBy(),
				result.createdAt()
		);
	}
}
