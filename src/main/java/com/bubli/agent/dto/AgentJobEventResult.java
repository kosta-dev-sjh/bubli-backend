package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentJobEvent;

import java.time.Instant;
import java.util.UUID;

public record AgentJobEventResult(
		UUID id,
		UUID jobId,
		String eventType,
		String message,
		Instant createdAt
) {
	public static AgentJobEventResult from(AgentJobEvent event) {
		return new AgentJobEventResult(
				event.getId(),
				event.getJobId(),
				event.getEventType(),
				event.getMessage(),
				event.getCreatedAt()
		);
	}
}
