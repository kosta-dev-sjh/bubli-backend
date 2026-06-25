package com.bubli.agent.dto;

import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;

import java.time.Instant;
import java.util.UUID;

public record AgentJobResponse(
		UUID id,
		UUID requestedByUserId,
		UUID roomId,
		UUID resourceId,
		AgentJobType jobType,
		AgentJobStatus status,
		int retryCount,
		String errorCode,
		String errorMessage,
		Instant startedAt,
		Instant finishedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static AgentJobResponse from(AgentJobResult result) {
		return new AgentJobResponse(
				result.id(),
				result.requestedByUserId(),
				result.roomId(),
				result.resourceId(),
				result.jobType(),
				result.status(),
				result.retryCount(),
				result.errorCode(),
				result.errorMessage(),
				result.startedAt(),
				result.finishedAt(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
