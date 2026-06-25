package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceComment;

import java.time.Instant;
import java.util.UUID;

public record ResourceCommentResult(
		UUID id,
		UUID resourceId,
		UUID authorId,
		UUID parentId,
		String body,
		Instant createdAt,
		Instant updatedAt
) {
	public static ResourceCommentResult from(ResourceComment comment) {
		return new ResourceCommentResult(
				comment.getId(),
				comment.getResourceId(),
				comment.getAuthorId(),
				comment.getParentId(),
				comment.getBody(),
				comment.getCreatedAt(),
				comment.getUpdatedAt()
		);
	}
}
