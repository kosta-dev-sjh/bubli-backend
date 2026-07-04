package com.bubli.personal.calendar.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarTransactionBoundaryTest {

	@Test
	void groupedEventsAllowsTokenRefreshWrites() throws NoSuchMethodException {
		Transactional transactional = GoogleCalendarGroupService.class
				.getMethod(
						"getGroupedEvents",
						UUID.class,
						UUID.class,
						Instant.class,
						Instant.class,
						List.class,
						Integer.class
				)
				.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isFalse();
	}

	@Test
	void googleCalendarListAllowsTokenRefreshWrites() throws NoSuchMethodException {
		Transactional transactional = GoogleCalendarEventService.class
				.getMethod("getGoogleCalendars", UUID.class)
				.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isFalse();
	}
}
