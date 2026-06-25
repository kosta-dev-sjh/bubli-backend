package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.entity.ResourceVersion;

import java.time.Instant;
import java.util.UUID;

public record ResourceVersionResult(
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
	public static ResourceVersionResult from(ResourceVersion version, ResourceFile file) {
		return new ResourceVersionResult(
				version.getId(),
				version.getResourceId(),
				version.getVersionNo(),
				version.getFileId(),
				file.getStorageKey(),
				file.getOriginalName(),
				file.getMimeType(),
				file.getSizeBytes(),
				file.getChecksum(),
				version.getCreatedBy(),
				version.getCreatedAt()
		);
	}
}
