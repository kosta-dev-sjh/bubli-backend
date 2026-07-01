package com.bubli.agent.dto;

import com.bubli.agent.type.AgentCommandMode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record ProjectRoomAgentCommandRequest(
		@NotBlank String message,
		AgentCommandMode mode,
		List<UUID> resourceIds
) {
	public ProjectRoomAgentCommandRequest {
		resourceIds = resourceIds == null ? List.of() : List.copyOf(resourceIds);
	}
}
