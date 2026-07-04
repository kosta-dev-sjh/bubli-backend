package com.bubli.user.dto;

import java.util.UUID;
import java.time.Instant;

public record UpdateUserPreferenceCommand(
		String theme,
		String defaultHomeType,
		UUID defaultProjectRoomId,
		String jobRole,
		Instant onboardingCompletedAt
) {
}
