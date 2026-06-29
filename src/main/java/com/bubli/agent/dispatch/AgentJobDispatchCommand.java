package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobType;

import java.util.Map;
import java.util.UUID;

public record AgentJobDispatchCommand(
		UUID jobId,
		UUID requestedByUserId,
		UUID roomId,
		UUID resourceId,
		AgentJobType jobType,
		Map<String, Object> requestPayload
) {
	public AgentJobDispatchCommand(UUID jobId, UUID requestedByUserId, UUID roomId, UUID resourceId, AgentJobType jobType) {
		this(jobId, requestedByUserId, roomId, resourceId, jobType, null);
	}

	public static AgentJobDispatchCommand from(AgentJob agentJob) {
		return new AgentJobDispatchCommand(
				agentJob.getId(),
				agentJob.getRequestedByUserId(),
				agentJob.getRoomId(),
				agentJob.getResourceId(),
				agentJob.getJobType(),
				agentJob.getRequestPayload()
		);
	}
}
