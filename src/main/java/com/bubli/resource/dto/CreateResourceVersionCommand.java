package com.bubli.resource.dto;

public record CreateResourceVersionCommand(
		String storageKey,
		String originalName,
		String mimeType,
		Long sizeBytes,
		String checksum
) {
}
