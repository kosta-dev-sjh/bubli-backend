package com.bubli.activity.repository;

import com.bubli.activity.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

	Optional<ActivityLog> findByUserIdAndLocalActivityId(UUID userId, String localActivityId);

	List<ActivityLog> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByStartedAtDesc(
			UUID userId,
			Instant from,
			Instant to
	);

	List<ActivityLog> findByUserIdAndStartedAtGreaterThanEqualAndStartedAtLessThanOrderByDurationSecondsDesc(
			UUID userId,
			Instant from,
			Instant to,
			Pageable pageable
	);
}
