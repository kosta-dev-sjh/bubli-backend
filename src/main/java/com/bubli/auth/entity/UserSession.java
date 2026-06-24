package com.bubli.auth.entity;

import com.bubli.auth.type.ClientType;
import com.bubli.auth.type.SessionStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "user_sessions",
	uniqueConstraints = @UniqueConstraint(name = "uk_user_sessions_user_client", columnNames = {"user_id", "client_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSession {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "refresh_token", nullable = false, unique = true, length = 500)
	private String refreshToken;

	@Enumerated(EnumType.STRING)
	@Column(name = "client_type", nullable = false, length = 30)
	private ClientType clientType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SessionStatus status = SessionStatus.ACTIVE;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "last_used_at")
	private Instant lastUsedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

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
