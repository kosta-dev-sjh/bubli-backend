package com.bubli.agent.dto;

import java.util.List;

public record PersonalAgentMemoryRequest(
		List<PersonalAgentMemoryMessage> recentMessages,
		List<PersonalAgentMemorySummary> summaries
) {
	public PersonalAgentMemoryRequest {
		recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
		summaries = summaries == null ? List.of() : List.copyOf(summaries);
	}

	public PersonalAgentMemoryInput toInput() {
		return new PersonalAgentMemoryInput(recentMessages, summaries);
	}
}
