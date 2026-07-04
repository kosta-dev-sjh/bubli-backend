package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.ProjectRoomGoogleCalendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRoomGoogleCalendarRepository extends JpaRepository<ProjectRoomGoogleCalendar, UUID> {

	Optional<ProjectRoomGoogleCalendar> findByUserIdAndRoomId(UUID userId, UUID roomId);

	List<ProjectRoomGoogleCalendar> findByUserIdAndRoomIdIn(UUID userId, Collection<UUID> roomIds);
}
