package com.bubli.agent.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ProjectRoomGroundingEvidence(
		ProjectRoomGroundingSourceType sourceType,
		UUID id,
		Map<String, Object> metadata
) {

	public ProjectRoomGroundingEvidence {
		metadata = metadata == null ? Map.of() : new LinkedHashMap<>(metadata);
	}

	public Map<String, Object> toPayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("sourceType", sourceType.name());
		payload.put("id", id);
		payload.put("metadata", metadata);
		return payload;
	}
}
