package com.bubli.user.dto;

import com.bubli.user.type.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateNotificationPreferencesRequest(
		@NotNull @Size(min = 1) List<@Valid Item> items
) {
	public UpdateNotificationPreferencesCommand toCommand() {
		return new UpdateNotificationPreferencesCommand(
				items.stream()
						.map(item -> new UpdateNotificationPreferencesCommand.Item(
								item.notificationType(),
								item.enabled()
						))
						.toList()
		);
	}

	public record Item(
			@NotNull NotificationType notificationType,
			@NotNull Boolean enabled
	) {
	}
}
