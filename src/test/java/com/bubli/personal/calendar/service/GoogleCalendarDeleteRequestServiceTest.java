package com.bubli.personal.calendar.service;

import com.bubli.personal.calendar.entity.GoogleCalendarDeleteRequest;
import com.bubli.personal.calendar.repository.GoogleCalendarDeleteRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarDeleteRequestServiceTest {

	@Mock
	GoogleCalendarDeleteRequestRepository repository;

	@InjectMocks
	GoogleCalendarDeleteRequestService service;

	@Test
	void rememberFailedAttemptStoresCalendarIdWithEventId() {
		UUID userId = UUID.randomUUID();
		given(repository.findByUserIdAndGoogleCalendarIdAndGoogleEventId(
				userId,
				"room-calendar-id",
				"google-event-1"
		)).willReturn(Optional.empty());

		service.rememberFailedAttempt(userId, " room-calendar-id ", " google-event-1 ");

		verify(repository).save(org.mockito.ArgumentMatchers.argThat(request ->
				userId.equals(request.getUserId())
						&& "room-calendar-id".equals(request.getGoogleCalendarId())
						&& "google-event-1".equals(request.getGoogleEventId())
						&& request.getAttemptCount() == 1
		));
	}

	@Test
	void findPendingGoogleEventIdsSearchesWithinOneCalendar() {
		UUID userId = UUID.randomUUID();
		given(repository.findByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				"room-calendar-id",
				List.of("google-event-1", "google-event-2")
		)).willReturn(List.of(GoogleCalendarDeleteRequest.create(userId, "room-calendar-id", "google-event-2")));

		Set<String> result = service.findPendingGoogleEventIds(
				userId,
				"room-calendar-id",
				List.of("google-event-1", "google-event-2")
		);

		assertThat(result).containsExactly("google-event-2");
	}

	@Test
	void markSucceededDeletesWithinOneCalendar() {
		UUID userId = UUID.randomUUID();

		service.markSucceeded(userId, "room-calendar-id", List.of("google-event-1", "google-event-1", " "));

		verify(repository).deleteByUserIdAndGoogleCalendarIdAndGoogleEventIdIn(
				userId,
				"room-calendar-id",
				List.of("google-event-1")
		);
	}
}
