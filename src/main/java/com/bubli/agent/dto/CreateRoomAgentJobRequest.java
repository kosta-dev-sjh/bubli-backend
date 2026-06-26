package com.bubli.agent.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRoomAgentJobRequest(
		@NotNull UUID roomId
) {
}
