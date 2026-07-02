package com.bubli.personal.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "google_calendar_delete_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleCalendarDeleteRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "google_event_id", nullable = false, length = 255)
	private String googleEventId;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "last_attempted_at")
	private Instant lastAttemptedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static GoogleCalendarDeleteRequest create(UUID userId, String googleEventId) {
		GoogleCalendarDeleteRequest request = new GoogleCalendarDeleteRequest();
		request.userId = userId;
		request.googleEventId = googleEventId;
		return request;
	}

	public void recordAttempt() {
		this.attemptCount += 1;
		this.lastAttemptedAt = Instant.now();
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
