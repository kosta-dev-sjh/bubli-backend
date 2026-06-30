package com.bubli.work.schedule.repository;

import com.bubli.work.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID>, JpaSpecificationExecutor<Schedule> {

	List<Schedule> findByOwnerUserIdAndStartsAtBetweenOrderByStartsAtAsc(UUID ownerUserId, Instant from, Instant to);

	List<Schedule> findByRoomIdAndStartsAtBetweenOrderByStartsAtAsc(UUID roomId, Instant from, Instant to);
}
