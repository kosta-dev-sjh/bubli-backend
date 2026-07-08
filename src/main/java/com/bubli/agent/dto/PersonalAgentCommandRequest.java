package com.bubli.agent.dto;

import com.bubli.agent.type.AgentCommandMode;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record PersonalAgentCommandRequest(
		@NotBlank String message,
		AgentCommandMode mode,
		List<UUID> resourceIds,
		PersonalAgentMemoryRequest memory
) {
	public PersonalAgentCommandRequest {
		resourceIds = resourceIds == null ? List.of() : List.copyOf(resourceIds);
		memory = memory == null ? new PersonalAgentMemoryRequest(List.of(), List.of()) : memory;
	}
}
