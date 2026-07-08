package com.bubli.agent.dto;

import java.time.Instant;

public record PersonalAgentMemoryMessage(
		String role,
		String text,
		Instant createdAt
) {
}
