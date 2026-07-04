package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.ProjectRoomGoogleCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRoomGoogleCalendarRepository extends JpaRepository<ProjectRoomGoogleCalendar, UUID> {

	Optional<ProjectRoomGoogleCalendar> findByUserIdAndRoomId(UUID userId, UUID roomId);

	List<ProjectRoomGoogleCalendar> findByUserIdAndRoomIdIn(UUID userId, Collection<UUID> roomIds);

	@Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
	void lockUserRoomMapping(@Param("lockKey") String lockKey);

	@Modifying
	@Query(value = """
			INSERT INTO project_room_google_calendars (
			    id,
			    user_id,
			    room_id,
			    google_calendar_id,
			    calendar_name,
			    created_at
			)
			VALUES (
			    :id,
			    :userId,
			    :roomId,
			    :googleCalendarId,
			    :calendarName,
			    CURRENT_TIMESTAMP
			)
			ON CONFLICT (user_id, room_id) DO NOTHING
			""", nativeQuery = true)
	int insertIfAbsent(
			@Param("id") UUID id,
			@Param("userId") UUID userId,
			@Param("roomId") UUID roomId,
			@Param("googleCalendarId") String googleCalendarId,
			@Param("calendarName") String calendarName
	);
}
