package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceFile;

import java.util.UUID;

public record ResourceFileResponse(
		UUID id,
		String originalName,
		String mimeType,
		Long sizeBytes
) {
	public static ResourceFileResponse from(ResourceFile resourceFile) {
		return new ResourceFileResponse(
				resourceFile.getId(),
				resourceFile.getOriginalName(),
				resourceFile.getMimeType(),
				resourceFile.getSizeBytes()
		);
	}
}
