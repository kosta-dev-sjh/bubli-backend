package com.bubli.personal.calendar.entity;

import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "google_calendar_connections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleCalendarConnection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(name = "google_account_email", length = 255)
	private String googleAccountEmail;

	@Column(name = "access_token", nullable = false, columnDefinition = "text")
	private String accessToken;

	@Column(name = "refresh_token", columnDefinition = "text")
	private String refreshToken;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GoogleCalendarConnectionStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static GoogleCalendarConnection create(
			UUID userId,
			String googleAccountEmail,
			String accessToken,
			String refreshToken,
			Instant expiresAt
	) {
		GoogleCalendarConnection connection = new GoogleCalendarConnection();
		connection.userId = userId;
		connection.googleAccountEmail = googleAccountEmail;
		connection.accessToken = accessToken;
		connection.refreshToken = refreshToken;
		connection.expiresAt = expiresAt;
		connection.status = GoogleCalendarConnectionStatus.ACTIVE;
		return connection;
	}

	public void updateTokens(String googleAccountEmail, String accessToken, String refreshToken, Instant expiresAt) {
		this.googleAccountEmail = googleAccountEmail;
		this.accessToken = accessToken;
		if (refreshToken != null && !refreshToken.isBlank()) {
			this.refreshToken = refreshToken;
		}
		this.expiresAt = expiresAt;
		this.status = GoogleCalendarConnectionStatus.ACTIVE;
	}

	public void refreshAccessToken(String accessToken, Instant expiresAt) {
		this.accessToken = accessToken;
		this.expiresAt = expiresAt;
		this.status = GoogleCalendarConnectionStatus.ACTIVE;
	}

	public void revoke() {
		this.status = GoogleCalendarConnectionStatus.REVOKED;
	}

	public boolean isActive() {
		return status == GoogleCalendarConnectionStatus.ACTIVE;
	}

	public boolean shouldRefresh(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(now.plusSeconds(60));
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
