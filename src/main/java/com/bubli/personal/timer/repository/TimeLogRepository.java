package com.bubli.personal.timer.repository;

import com.bubli.personal.timer.entity.TimeLog;
import com.bubli.personal.timer.type.TimeLogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {

	Optional<TimeLog> findByIdAndUserId(UUID id, UUID userId);

	Page<TimeLog> findAllByUserId(UUID userId, Pageable pageable);

	boolean existsByUserIdAndStatus(UUID userId, TimeLogStatus status);

	Optional<TimeLog> findByIdempotencyKey(String idempotencyKey);
}
