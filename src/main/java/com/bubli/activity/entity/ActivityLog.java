package com.bubli.activity.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "activity_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "app_name", length = 120)
	private String appName;

	@Column(name = "window_title", length = 500)
	private String windowTitle;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Column(name = "duration_seconds", nullable = false)
	private Long durationSeconds = 0L;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static ActivityLog create(
			UUID userId,
			UUID roomId,
			String appName,
			String windowTitle,
			Instant startedAt,
			Instant endedAt,
			Long durationSeconds
	) {
		ActivityLog activityLog = new ActivityLog();
		activityLog.userId = userId;
		activityLog.roomId = roomId;
		activityLog.appName = appName;
		activityLog.windowTitle = windowTitle;
		activityLog.startedAt = startedAt;
		activityLog.endedAt = endedAt;
		activityLog.durationSeconds = durationSeconds == null ? 0L : durationSeconds;
		return activityLog;
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
