package com.bubli.personal.timer.repository;

import com.bubli.personal.timer.entity.TimeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TimeLogRepository extends JpaRepository<TimeLog, UUID> {

	Optional<TimeLog> findByIdempotencyKey(String idempotencyKey);
}
