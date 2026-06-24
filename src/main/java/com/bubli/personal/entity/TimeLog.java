package com.bubli.personal.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.personal.type.TimeLogStatus;
import com.bubli.personal.type.TimerType;
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
public class TimeLog extends BaseTimeEntity {

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

}
