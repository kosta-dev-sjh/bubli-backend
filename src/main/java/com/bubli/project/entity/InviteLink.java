package com.bubli.project.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Getter
@Entity
@Table(name = "invite_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteLink {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "created_by_user_id", nullable = false)
	private UUID createdByUserId;

	@Column(nullable = false, length = 100, unique = true)
	private String token;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static InviteLink create(UUID roomId, UUID createdByUserId, int expiresInHours) {
		InviteLink link = new InviteLink();
		link.roomId = roomId;
		link.createdByUserId = createdByUserId;
		link.token = UUID.randomUUID().toString();
		link.expiresAt = Instant.now().plus(expiresInHours, ChronoUnit.HOURS);
		return link;
	}

	public boolean isExpired() {
		return Instant.now().isAfter(expiresAt);
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}
}
