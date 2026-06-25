package com.bubli.resource.dto;

import com.bubli.resource.type.ResourceSummaryStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceSummaryResponse(
		UUID id,
		UUID resourceId,
		UUID jobId,
		String summaryJson,
		String checklistJson,
		ResourceSummaryStatus status,
		String promptVersion,
		String schemaVersion,
		String modelName,
		Instant createdAt,
		Instant updatedAt
) {
	public static ResourceSummaryResponse from(ResourceSummaryResult result) {
		return new ResourceSummaryResponse(
				result.id(),
				result.resourceId(),
				result.jobId(),
				result.summaryJson(),
				result.checklistJson(),
				result.status(),
				result.promptVersion(),
				result.schemaVersion(),
				result.modelName(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
