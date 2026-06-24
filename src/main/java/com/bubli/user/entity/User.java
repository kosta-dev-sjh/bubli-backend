package com.bubli.user.entity;

import com.bubli.user.type.UserStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "google_sub", nullable = false, unique = true, length = 120)
	private String googleSub;

	@Column(name = "bubli_id", unique = true, length = 40)
	private String bubliId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Column(length = 20)
	private String locale;

	@Column(length = 50)
	private String timezone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static User createGoogleUser(String googleSub, String bubliId, String name, String avatarUrl,
			String locale, String timezone) {
		User user = new User();
		user.googleSub = googleSub;
		user.bubliId = bubliId;
		user.name = name;
		user.avatarUrl = avatarUrl;
		user.locale = locale;
		user.timezone = timezone;
		user.status = UserStatus.ACTIVE;
		return user;
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
