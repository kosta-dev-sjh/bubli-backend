package com.bubli.agent.dto;

import com.bubli.agent.type.AgentCommandMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProjectRoomRagEvaluationRequest(
		@NotNull UUID roomId,
		@NotBlank String message,
		String locale,
		AgentCommandMode mode,
		@Min(1) @Max(20) Integer topK,
		List<UUID> resourceIds
) {

	public ProjectRoomRagEvaluationRequest {
		mode = mode == null ? AgentCommandMode.ANSWER : mode;
		locale = locale == null || locale.isBlank() ? "ko-KR" : locale;
		resourceIds = resourceIds == null ? List.of() : List.copyOf(resourceIds);
	}
}
