package com.bubli.resource.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResourceRelatedResponse(
		UUID id,
		UUID resourceId,
		UUID relatedResourceId,
		String reason,
		BigDecimal score,
		Instant createdAt,
		ResourceResponse relatedResource
) {
	public static ResourceRelatedResponse from(ResourceRelatedResult result) {
		return new ResourceRelatedResponse(
				result.id(),
				result.resourceId(),
				result.relatedResourceId(),
				result.reason(),
				result.score(),
				result.createdAt(),
				ResourceResponse.from(result.relatedResource())
		);
	}
}
