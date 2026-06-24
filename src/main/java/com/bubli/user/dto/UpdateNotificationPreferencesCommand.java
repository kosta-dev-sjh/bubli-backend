package com.bubli.user.dto;

import com.bubli.user.type.NotificationType;

import java.util.List;

public record UpdateNotificationPreferencesCommand(
		List<Item> items
) {
	public UpdateNotificationPreferencesCommand {
		items = List.copyOf(items);
	}

	public record Item(
			NotificationType notificationType,
			boolean enabled
	) {
	}
}
