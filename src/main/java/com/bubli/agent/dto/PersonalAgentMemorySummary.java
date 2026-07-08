package com.bubli.agent.dto;

import java.time.Instant;

public record PersonalAgentMemorySummary(
		String summary,
		Instant fromCreatedAt,
		Instant toCreatedAt
) {
}
