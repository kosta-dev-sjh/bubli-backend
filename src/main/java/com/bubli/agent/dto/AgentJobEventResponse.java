package com.bubli.agent.dto;

import java.time.Instant;
import java.util.UUID;

public record AgentJobEventResponse(
		UUID id,
		UUID jobId,
		String eventType,
		String message,
		Instant createdAt
) {
	public static AgentJobEventResponse from(AgentJobEventResult result) {
		return new AgentJobEventResponse(
				result.id(),
				result.jobId(),
				result.eventType(),
				result.message(),
				result.createdAt()
		);
	}
}
