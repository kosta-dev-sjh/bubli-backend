package com.bubli.user.dto;

import com.bubli.user.entity.UserPreference;

import java.time.Instant;
import java.util.UUID;

public record UserPreferenceResult(
		UUID userId,
		String theme,
		String defaultHomeType,
		String jobRole,
		UUID defaultProjectRoomId,
		Instant onboardingCompletedAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static UserPreferenceResult empty(UUID userId) {
		return new UserPreferenceResult(userId, null, null, null, null, null, null, null);
	}

	public static UserPreferenceResult from(UserPreference preference) {
		return new UserPreferenceResult(
				preference.getUserId(),
				preference.getTheme(),
				preference.getDefaultHomeType(),
				preference.getJobRole(),
				preference.getDefaultProjectRoomId(),
				preference.getOnboardingCompletedAt(),
				preference.getCreatedAt(),
				preference.getUpdatedAt()
		);
	}
}
