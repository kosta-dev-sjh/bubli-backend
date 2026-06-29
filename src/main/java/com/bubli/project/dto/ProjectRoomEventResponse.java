package com.bubli.project.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record ProjectRoomEventResponse(
		UUID eventId,
		String eventType,
		UUID roomId,
		Long sequence,
		Instant occurredAt,
		ProjectRoomEventActorResponse actor,
		JsonNode payload
) {
}
