package com.bubli.agent.dto;

import com.bubli.resource.dto.ResourceSearchHit;

import java.util.List;
import java.util.Map;

public record ProjectRoomRagEvaluationResponse(
		boolean grounded,
		boolean retrievalFailed,
		String retrievalFailureReason,
		double ragMaxSimilarity,
		int ragHitCount,
		int evidenceCount,
		List<String> sourceTypes,
		List<String> retrievalModes,
		List<ResourceSearchHit> ragHits,
		List<Map<String, Object>> evidenceItems
) {

	public static ProjectRoomRagEvaluationResponse from(ProjectRoomGroundingContext context) {
		return new ProjectRoomRagEvaluationResponse(
				context.grounded(),
				context.retrievalFailed(),
				context.retrievalFailureReason(),
				context.ragMaxSimilarity(),
				context.ragHits().size(),
				context.evidenceItems().size(),
				context.sourceTypes().stream()
						.map(Enum::name)
						.toList(),
				context.retrievalModes(),
				context.ragHits(),
				context.evidenceItems().stream()
						.map(ProjectRoomGroundingEvidence::toPayload)
						.toList()
		);
	}
}
