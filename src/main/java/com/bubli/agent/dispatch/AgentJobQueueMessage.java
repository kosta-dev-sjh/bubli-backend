package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AgentJobQueueMessage(
		UUID jobId,
		UUID requestedByUserId,
		UUID roomId,
		UUID resourceId,
		AgentJobType jobType,
		Map<String, Object> requestPayload,
		Instant enqueuedAt
) {
	public AgentJobQueueMessage(
			UUID jobId,
			UUID requestedByUserId,
			UUID roomId,
			UUID resourceId,
			AgentJobType jobType,
			Instant enqueuedAt
	) {
		this(jobId, requestedByUserId, roomId, resourceId, jobType, null, enqueuedAt);
	}

	public static AgentJobQueueMessage from(AgentJobDispatchCommand command, Instant enqueuedAt) {
		return new AgentJobQueueMessage(
				command.jobId(),
				command.requestedByUserId(),
				command.roomId(),
				command.resourceId(),
				command.jobType(),
				command.requestPayload(),
				enqueuedAt
		);
	}
}
