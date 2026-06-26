package com.bubli.agent.dto;

import com.bubli.agent.type.AgentSuggestionStatus;
import jakarta.validation.constraints.Size;

public record UpdateAgentSuggestionRequest(
		AgentSuggestionStatus status,

		@Size(max = 20000, message = "제안 내용은 20000자 이하여야 합니다.")
		String payloadJson,

		@Size(max = 20000, message = "제안 근거는 20000자 이하여야 합니다.")
		String evidenceJson
) {
	public UpdateAgentSuggestionCommand toCommand() {
		return new UpdateAgentSuggestionCommand(
				status,
				payloadJson == null ? null : payloadJson.trim(),
				evidenceJson == null ? null : evidenceJson.trim()
		);
	}
}
