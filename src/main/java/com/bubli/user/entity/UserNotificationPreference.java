package com.bubli.user.entity;

import com.bubli.user.type.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "user_notification_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationPreference {
	@EmbeddedId
	private UserNotificationPreferenceId id;

	@Column(nullable = false)
	private boolean enabled = true;

	private UserNotificationPreference(UUID userId, NotificationType notificationType, boolean enabled) {
		this.id = UserNotificationPreferenceId.of(userId, notificationType);
		this.enabled = enabled;
	}

	public static UserNotificationPreference create(UUID userId, NotificationType notificationType, boolean enabled) {
		return new UserNotificationPreference(userId, notificationType, enabled);
	}

	public void updateEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public UUID getUserId() {
		return id.getUserId();
	}

	public NotificationType getNotificationType() {
		return id.getNotificationType();
	}
}
