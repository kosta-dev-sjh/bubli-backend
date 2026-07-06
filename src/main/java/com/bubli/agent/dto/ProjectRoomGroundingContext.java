package com.bubli.agent.dto;

import com.bubli.resource.dto.ResourceSearchHit;

import java.util.List;
import java.util.UUID;

public record ProjectRoomGroundingContext(
		boolean grounded,
		List<ResourceSearchHit> ragHits,
		double ragMaxSimilarity,
		List<ProjectRoomGroundingEvidence> evidenceItems,
		String promptBlock
) {

	public ProjectRoomGroundingContext {
		ragHits = ragHits == null ? List.of() : List.copyOf(ragHits);
		evidenceItems = evidenceItems == null ? List.of() : List.copyOf(evidenceItems);
		promptBlock = promptBlock == null ? "" : promptBlock;
	}

	public static ProjectRoomGroundingContext ungrounded() {
		return new ProjectRoomGroundingContext(false, List.of(), 0.0D, List.of(), "");
	}

	public List<ProjectRoomGroundingSourceType> sourceTypes() {
		return evidenceItems.stream()
				.map(ProjectRoomGroundingEvidence::sourceType)
				.distinct()
				.toList();
	}

	public List<UUID> resourceIds() {
		return ragHits.stream()
				.map(ResourceSearchHit::resourceId)
				.distinct()
				.toList();
	}

	public UUID firstResourceId() {
		return resourceIds().stream().findFirst().orElse(null);
	}

	public List<UUID> taskIds() {
		return ids(ProjectRoomGroundingSourceType.TASK);
	}

	public List<UUID> wbsItemIds() {
		return ids(ProjectRoomGroundingSourceType.WBS);
	}

	public List<UUID> scheduleIds() {
		return ids(ProjectRoomGroundingSourceType.SCHEDULE);
	}

	public List<UUID> agentSuggestionIds() {
		return ids(ProjectRoomGroundingSourceType.AGENT_SUGGESTION);
	}

	public boolean hasDocumentEvidence() {
		return !ragHits.isEmpty();
	}

	private List<UUID> ids(ProjectRoomGroundingSourceType sourceType) {
		return evidenceItems.stream()
				.filter(evidence -> evidence.sourceType() == sourceType)
				.map(ProjectRoomGroundingEvidence::id)
				.distinct()
				.toList();
	}
}
