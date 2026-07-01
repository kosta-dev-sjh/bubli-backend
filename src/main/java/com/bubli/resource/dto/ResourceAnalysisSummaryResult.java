package com.bubli.resource.dto;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ResourceAnalysisSummaryResult(
		UUID resourceId,
		String title,
		String summary,
		Instant updatedAt
) {

	public static ResourceAnalysisSummaryResult from(ResourceSummary summary, Resource resource) {
		return new ResourceAnalysisSummaryResult(
				summary.getResourceId(),
				resource.getTitle(),
				summaryText(summary.getSummaryJson()),
				summary.getUpdatedAt()
		);
	}

	private static String summaryText(Map<String, Object> summaryJson) {
		if (summaryJson == null || summaryJson.isEmpty()) {
			return "분석 완료";
		}
		for (String key : List.of("summary", "title", "description", "raw")) {
			Object value = summaryJson.get(key);
			if (value != null && !value.toString().isBlank()) {
				return value.toString();
			}
		}
		return "분석 완료";
	}
}
