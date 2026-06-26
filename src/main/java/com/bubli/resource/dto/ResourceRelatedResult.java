package com.bubli.resource.dto;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceRelation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResourceRelatedResult(
		UUID id,
		UUID resourceId,
		UUID relatedResourceId,
		String reason,
		BigDecimal score,
		Instant createdAt,
		ResourceResult relatedResource
) {
	public static ResourceRelatedResult from(ResourceRelation relation, Resource relatedResource) {
		return new ResourceRelatedResult(
				relation.getId(),
				relation.getResourceId(),
				relation.getRelatedResourceId(),
				relation.getReason(),
				relation.getScore(),
				relation.getCreatedAt(),
				ResourceResult.from(relatedResource)
		);
	}
}
