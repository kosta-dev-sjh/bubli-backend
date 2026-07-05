package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.GoogleCalendarDeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoogleCalendarDeleteRequestRepository extends JpaRepository<GoogleCalendarDeleteRequest, UUID> {

	Optional<GoogleCalendarDeleteRequest> findByUserIdAndGoogleCalendarIdAndGoogleEventId(
			UUID userId,
			String googleCalendarId,
			String googleEventId
	);

	List<GoogleCalendarDeleteRequest> findByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
			UUID userId,
			String googleCalendarId,
			Collection<String> googleEventIds
	);

	void deleteByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
			UUID userId,
			String googleCalendarId,
			Collection<String> googleEventIds
	);
}
