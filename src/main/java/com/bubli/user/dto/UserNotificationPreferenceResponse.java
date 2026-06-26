package com.bubli.user.dto;

import com.bubli.user.type.NotificationType;

import java.util.List;
import java.util.UUID;

public record UserNotificationPreferenceResponse(
		UUID userId,
		List<Item> items
) {
	public static UserNotificationPreferenceResponse from(UserNotificationPreferencesResult result) {
		return new UserNotificationPreferenceResponse(
				result.userId(),
				result.items().stream()
						.map(Item::from)
						.toList()
		);
	}

	public record Item(
			NotificationType notificationType,
			boolean enabled
	) {
		public static Item from(UserNotificationPreferenceResult result) {
			return new Item(
					result.notificationType(),
					result.enabled()
			);
		}
	}
}
