package com.bubli.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_notification_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotificationPreference {
	@EmbeddedId
	private UserNotificationPreferenceId id;

	@Column(nullable = false)
	private boolean enabled = true;
}
