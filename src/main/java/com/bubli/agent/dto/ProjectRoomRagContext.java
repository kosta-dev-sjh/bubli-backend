package com.bubli.agent.dto;

import com.bubli.resource.dto.ResourceSearchHit;

import java.util.List;
import java.util.UUID;

public record ProjectRoomRagContext(
		boolean grounded,
		List<ResourceSearchHit> hits,
		double maxSimilarity,
		String promptBlock
) {

	public ProjectRoomRagContext {
		hits = hits == null ? List.of() : List.copyOf(hits);
		promptBlock = promptBlock == null ? "" : promptBlock;
	}

	public static ProjectRoomRagContext ungrounded() {
		return new ProjectRoomRagContext(false, List.of(), 0.0D, "");
	}

	public List<UUID> resourceIds() {
		return hits.stream()
				.map(ResourceSearchHit::resourceId)
				.distinct()
				.toList();
	}

	public UUID firstResourceId() {
		return resourceIds().stream().findFirst().orElse(null);
	}
}
