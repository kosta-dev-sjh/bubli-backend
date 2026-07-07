package com.bubli.agent.dto;

import java.util.List;

public record PersonalAgentCommandResponse(
		PersonalAgentMessageResponse message,
		List<PersonalAgentSuggestionResponse> suggestions
) {
	public PersonalAgentCommandResponse {
		suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
	}
}
