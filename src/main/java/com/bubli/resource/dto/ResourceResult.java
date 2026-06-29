package com.bubli.resource.dto;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;

import java.time.Instant;
import java.util.UUID;

public record ResourceResult(
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
	public static ResourceResult from(Resource resource) {
		return new ResourceResult(
				resource.getId(),
				resource.getOwnerId(),
				resource.getRoomId(),
				resource.getTitle(),
				resource.getKind(),
				resource.getVisibility(),
				resource.getStatus(),
				resource.getCreatedAt(),
				resource.getUpdatedAt()
		);
	}
}
