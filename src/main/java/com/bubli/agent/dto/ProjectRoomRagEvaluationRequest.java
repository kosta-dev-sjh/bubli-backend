package com.bubli.agent.dto;

import com.bubli.agent.type.AgentCommandMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProjectRoomRagEvaluationRequest(
		@NotNull UUID roomId,
		@NotBlank String message,
		String locale,
		AgentCommandMode mode
) {

	public ProjectRoomRagEvaluationRequest {
		mode = mode == null ? AgentCommandMode.ANSWER : mode;
		locale = locale == null || locale.isBlank() ? "ko-KR" : locale;
	}
}
