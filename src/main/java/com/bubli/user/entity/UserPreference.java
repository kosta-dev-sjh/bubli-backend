package com.bubli.user.entity;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "user_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(length = 30)
	private String theme;

	@Column(name = "default_home_type", length = 30)
	private String defaultHomeType;

	@Column(name = "default_project_room_id")
	private UUID defaultProjectRoomId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static UserPreference create(UUID userId) {
		UserPreference preference = new UserPreference();
		preference.userId = userId;
		return preference;
	}

	public void update(String theme, String defaultHomeType, UUID defaultProjectRoomId) {
		if (theme != null) {
			this.theme = theme;
		}
		if (defaultHomeType != null) {
			this.defaultHomeType = defaultHomeType;
		}
		if (defaultProjectRoomId != null) {
			this.defaultProjectRoomId = defaultProjectRoomId;
		}
	}

	@PrePersist
	private void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	private void onUpdate() {
		this.updatedAt = Instant.now();
	}

}
