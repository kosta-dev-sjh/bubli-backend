package com.bubli.agent.dto;

import java.util.List;

public record PersonalAgentMemoryInput(
		List<PersonalAgentMemoryMessage> recentMessages,
		List<PersonalAgentMemorySummary> summaries
) {
	public PersonalAgentMemoryInput {
		recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
		summaries = summaries == null ? List.of() : List.copyOf(summaries);
	}
}
