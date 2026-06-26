package com.bubli.agent.dto;

import com.bubli.agent.type.AgentJobType;

import java.util.UUID;

public record CreateAgentJobCommand(
		UUID roomId,
		UUID resourceId,
		AgentJobType jobType
) {
}
