package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
		UUID id,
		UUID ownerId,
		UUID roomId,
		String title,
		ResourceKind kind,
		ResourceVisibility visibility,
		ResourceStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static ResourceResponse from(ResourceResult result) {
		return new ResourceResponse(
				result.id(),
				result.ownerId(),
				result.roomId(),
				result.title(),
				result.kind(),
				result.visibility(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
