package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.GoogleCalendarDeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarDeleteRequestRepository extends JpaRepository<GoogleCalendarDeleteRequest, UUID> {

	Optional<GoogleCalendarDeleteRequest> findByUserIdAndGoogleEventId(UUID userId, String googleEventId);

	List<GoogleCalendarDeleteRequest> findByUserIdAndGoogleEventIdIn(UUID userId, Collection<String> googleEventIds);

	void deleteByUserIdAndGoogleEventIdIn(UUID userId, Collection<String> googleEventIds);
}
