package com.bubli.work.schedule.repository;

import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID>, JpaSpecificationExecutor<Schedule> {

	List<Schedule> findByOwnerUserIdAndStartsAtBetweenOrderByStartsAtAsc(UUID ownerUserId, Instant from, Instant to);

	@Query("""
			select schedule
			from Schedule schedule
			where schedule.roomId = :roomId
			  and schedule.startsAt < :to
			  and (
			    (schedule.endsAt is null and schedule.startsAt >= :from)
			    or schedule.endsAt > :from
			  )
			order by schedule.startsAt asc, schedule.createdAt asc
			""")
	List<Schedule> findRoomOverlappingForRoom(
			@Param("roomId") UUID roomId,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@Query("""
			select schedule
			from Schedule schedule
			where schedule.roomId is null
			  and schedule.ownerUserId = :userId
			  and schedule.startsAt < :to
			  and (
			    (schedule.endsAt is null and schedule.startsAt >= :from)
			    or schedule.endsAt > :from
			  )
			order by schedule.startsAt asc, schedule.createdAt asc
			""")
	List<Schedule> findPersonalBetweenForUser(
			@Param("userId") UUID userId,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@Query("""
			select schedule
			from Schedule schedule
			where schedule.startsAt < :to
			  and (
			    (schedule.endsAt is null and schedule.startsAt >= :from)
			    or schedule.endsAt > :from
			  )
			  and (
			    (schedule.roomId is null and schedule.ownerUserId = :userId)
			    or schedule.roomId in :roomIds
			  )
			order by schedule.startsAt asc, schedule.createdAt asc
			""")
	List<Schedule> findVisibleBetweenForUser(
			@Param("userId") UUID userId,
			@Param("roomIds") Collection<UUID> roomIds,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	Optional<Schedule> findByOwnerUserIdAndGoogleCalendarIdAndGoogleEventId(
			UUID ownerUserId,
			String googleCalendarId,
			String googleEventId
	);

	List<Schedule> findByOwnerUserIdAndGoogleEventIdIn(UUID ownerUserId, List<String> googleEventIds);

	boolean existsByWbsItemId(UUID wbsItemId);

	List<Schedule> findByOwnerUserIdAndSyncStatusInAndStartsAtBetweenOrderByStartsAtAsc(
			UUID ownerUserId,
			List<ScheduleSyncStatus> syncStatuses,
			Instant from,
			Instant to
	);
}
