package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceVisibility;

import java.util.UUID;

public record UploadResourceCommand(
		String title,
		ResourceKind kind,
		ResourceVisibility visibility,
		UUID roomId,
		String originalName,
		String mimeType,
		byte[] content
) {
	public CreateResourceCommand toCreateResourceCommand() {
		return new CreateResourceCommand(title, kind, visibility, roomId);
	}
}
