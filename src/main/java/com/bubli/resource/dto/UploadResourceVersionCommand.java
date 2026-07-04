package com.bubli.resource.dto;

public record UploadResourceVersionCommand(
		String originalName,
		String mimeType,
		byte[] content
) {
}
