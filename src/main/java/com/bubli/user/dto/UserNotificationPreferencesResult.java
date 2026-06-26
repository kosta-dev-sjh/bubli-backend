package com.bubli.user.dto;

import java.util.List;
import java.util.UUID;

public record UserNotificationPreferencesResult(
		UUID userId,
		List<UserNotificationPreferenceResult> items
) {
	public UserNotificationPreferencesResult {
		items = List.copyOf(items);
	}
}
