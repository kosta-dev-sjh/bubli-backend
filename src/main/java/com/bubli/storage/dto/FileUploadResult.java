package com.bubli.storage.dto;

public record FileUploadResult(
		String storageKey,
		String originalName,
		String mimeType,
		long sizeBytes,
		String checksum
) {
}
