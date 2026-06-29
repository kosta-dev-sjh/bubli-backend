package com.bubli.resource.dto;

import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.type.ResourceSummaryStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceSummaryResult(
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
	public static ResourceSummaryResult from(ResourceSummary summary) {
		return new ResourceSummaryResult(
				summary.getId(),
				summary.getResourceId(),
				summary.getJobId(),
				String.valueOf(summary.getSummaryJson()),
				String.valueOf(summary.getChecklistJson()),
				ResourceSummaryStatus.valueOf(summary.getStatus().name()),
				summary.getPromptVersion(),
				summary.getSchemaVersion(),
				summary.getModelName(),
				summary.getCreatedAt(),
				summary.getUpdatedAt()
		);
	}
}
