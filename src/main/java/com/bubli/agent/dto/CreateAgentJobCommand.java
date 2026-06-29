package com.bubli.agent.dto;

import com.bubli.agent.type.AgentJobType;

import java.util.Map;
import java.util.UUID;

public record CreateAgentJobCommand(
		UUID roomId,
		UUID resourceId,
		AgentJobType jobType,
		Map<String, Object> requestPayload
) {
	public CreateAgentJobCommand(UUID roomId, UUID resourceId, AgentJobType jobType) {
		this(roomId, resourceId, jobType, null);
	}
}
