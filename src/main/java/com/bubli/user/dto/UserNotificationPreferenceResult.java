package com.bubli.user.dto;

import com.bubli.user.type.NotificationType;

public record UserNotificationPreferenceResult(
		NotificationType notificationType,
		boolean enabled
) {
}
