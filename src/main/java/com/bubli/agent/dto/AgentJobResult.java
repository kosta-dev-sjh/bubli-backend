package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;

import java.time.Instant;
import java.util.UUID;

public record AgentJobResult(
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
	public static AgentJobResult from(AgentJob agentJob) {
		return new AgentJobResult(
				agentJob.getId(),
				agentJob.getRequestedByUserId(),
				agentJob.getRoomId(),
				agentJob.getResourceId(),
				agentJob.getJobType(),
				agentJob.getStatus(),
				agentJob.getRetryCount(),
				agentJob.getErrorCode(),
				agentJob.getErrorMessage(),
				agentJob.getStartedAt(),
				agentJob.getFinishedAt(),
				agentJob.getCreatedAt(),
				agentJob.getUpdatedAt()
		);
	}
}
