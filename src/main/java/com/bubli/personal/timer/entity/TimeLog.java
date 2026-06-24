package com.bubli.personal.timer.entity;

import com.bubli.personal.timer.type.TimeLogStatus;
import com.bubli.personal.timer.type.TimerType;
import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "time_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeLog {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "room_id")
	private UUID roomId;

	@Enumerated(EnumType.STRING)
	@Column(name = "timer_type", nullable = false, length = 30)
	private TimerType timerType;

	@Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
	private String idempotencyKey;

	@Column(name = "recovered_from_time_log_id")
	private UUID recoveredFromTimeLogId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TimeLogStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "last_started_at")
	private Instant lastStartedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Column(name = "duration_seconds", nullable = false)
	private Long durationSeconds = 0L;

	@Column(name = "last_heartbeat_at")
	private Instant lastHeartbeatAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static TimeLog start(UUID userId, UUID roomId, TimerType timerType,
			String idempotencyKey, UUID recoveredFromTimeLogId, Instant now) {
		TimeLog timeLog = new TimeLog();
		timeLog.userId = userId;
		timeLog.roomId = roomId;
		timeLog.timerType = timerType == null ? TimerType.GENERAL : timerType;
		timeLog.idempotencyKey = idempotencyKey;
		timeLog.recoveredFromTimeLogId = recoveredFromTimeLogId;
		timeLog.status = TimeLogStatus.RUNNING;
		timeLog.startedAt = now;
		timeLog.lastStartedAt = now;
		timeLog.durationSeconds = 0L;
		timeLog.lastHeartbeatAt = now;
		return timeLog;
	}

	public void pause(Instant now) {
		this.durationSeconds += runningSecondsUntil(now);
		this.status = TimeLogStatus.PAUSED;
		this.lastStartedAt = null;
		this.lastHeartbeatAt = now;
	}

	public void resume(Instant now) {
		this.status = TimeLogStatus.RUNNING;
		this.lastStartedAt = now;
		this.lastHeartbeatAt = now;
	}

	public void stop(Instant now) {
		if (TimeLogStatus.RUNNING.equals(this.status)) {
			this.durationSeconds += runningSecondsUntil(now);
		}
		this.status = TimeLogStatus.ENDED;
		this.lastStartedAt = null;
		this.endedAt = now;
		this.lastHeartbeatAt = now;
	}

	public void heartbeat(Instant now) {
		this.lastHeartbeatAt = now;
	}

	private long runningSecondsUntil(Instant now) {
		if (this.lastStartedAt == null || now.isBefore(this.lastStartedAt)) {
			return 0L;
		}
		return Duration.between(this.lastStartedAt, now).getSeconds();
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
