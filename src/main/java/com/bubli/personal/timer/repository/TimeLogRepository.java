package com.bubli.personal.timer.repository;

import com.bubli.personal.timer.dto.TimeLogActivityRow;
import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.type.TimeLogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {

	Optional<TimeLog> findByIdAndUserId(UUID id, UUID userId);

	Page<TimeLog> findAllByUserId(UUID userId, Pageable pageable);

	Optional<TimeLog> findFirstByUserIdAndStatus(UUID userId, TimeLogStatus status);

	Optional<TimeLog> findFirstByUserIdAndRoomIdAndStatus(UUID userId, UUID roomId, TimeLogStatus status);

	Optional<TimeLog> findByIdempotencyKey(String idempotencyKey);

	@Query("""
			select new com.bubli.personal.timer.dto.TimeLogActivityRow(t.startedAt, t.durationSeconds)
			from TimeLog t
			where t.userId = :userId
			  and t.startedAt >= :from
			  and t.startedAt < :to
			""")
	List<TimeLogActivityRow> findActivityByUserIdAndStartedAtBetween(
			@Param("userId") UUID userId,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@Modifying
	@Query(value = """
			INSERT INTO time_logs (
			    id,
			    user_id,
			    room_id,
			    timer_type,
			    idempotency_key,
			    recovered_from_time_log_id,
			    status,
			    started_at,
			    last_started_at,
			    duration_seconds,
			    last_heartbeat_at,
			    created_at,
			    updated_at
			)
			VALUES (
			    :id,
			    :userId,
			    :roomId,
			    :timerType,
			    :idempotencyKey,
			    :recoveredFromTimeLogId,
			    'RUNNING',
			    :now,
			    :now,
			    0,
			    :now,
			    CURRENT_TIMESTAMP,
			    CURRENT_TIMESTAMP
			)
			ON CONFLICT DO NOTHING
			""", nativeQuery = true)
	int insertRunningIfNoConflict(
			@Param("id") UUID id,
			@Param("userId") UUID userId,
			@Param("roomId") UUID roomId,
			@Param("timerType") String timerType,
			@Param("idempotencyKey") String idempotencyKey,
			@Param("recoveredFromTimeLogId") UUID recoveredFromTimeLogId,
			@Param("now") Instant now
	);
}
