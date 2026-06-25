package com.bubli.resource.dto;

import java.time.Instant;
import java.util.UUID;

public record ResourceCommentResponse(
		UUID id,
		UUID resourceId,
		UUID authorId,
		UUID parentId,
		String body,
		Instant createdAt,
		Instant updatedAt
) {
	public static ResourceCommentResponse from(ResourceCommentResult result) {
		return new ResourceCommentResponse(
				result.id(),
				result.resourceId(),
				result.authorId(),
				result.parentId(),
				result.body(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
