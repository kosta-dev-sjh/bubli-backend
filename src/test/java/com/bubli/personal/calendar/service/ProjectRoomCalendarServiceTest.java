package com.bubli.personal.calendar.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.personal.calendar.dto.RoomCalendarResponse;
import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.repository.ProjectRoomGoogleCalendarRepository;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectRoomPublicService;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectRoomCalendarServiceTest {

	@Mock
	ProjectRoomGoogleCalendarRepository roomCalendarRepository;

	@Mock
	GoogleCalendarConnectionService connectionService;

	@Mock
	GoogleCalendarClient googleCalendarClient;

	@Mock
	ProjectRoomPublicService projectRoomPublicService;

	@InjectMocks
	ProjectRoomCalendarService projectRoomCalendarService;

	@Test
	void ensureRoomCalendarReturnsEmptyWhenGoogleCalendarInsertFails() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-05T01:00:00Z")
		);

		given(roomCalendarRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(Optional.empty());
		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(projectRoomPublicService.getProjectRoom(userId, roomId)).willReturn(room(userId, roomId));
		given(googleCalendarClient.insertCalendar("access-token", "A 프로젝트룸"))
				.willThrow(new BusinessException(ErrorCode.CALENDAR_502_001));

		assertThat(projectRoomCalendarService.ensureRoomCalendar(userId, roomId)).isEmpty();

		verify(roomCalendarRepository, never()).insertIfAbsent(
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(),
				anyString(),
				anyString()
		);
	}

	@Test
	void getRoomCalendarMarksReconsentRequiredWhenRoomCalendarCannotBeCreated() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		GoogleCalendarConnection connection = GoogleCalendarConnection.create(
				userId,
				"user@example.com",
				"access-token",
				"refresh-token",
				Instant.parse("2026-07-05T01:00:00Z")
		);

		given(projectRoomPublicService.getProjectRoom(userId, roomId)).willReturn(room(userId, roomId));
		given(connectionService.hasActiveConnection(userId)).willReturn(true);
		given(roomCalendarRepository.findByUserIdAndRoomId(userId, roomId)).willReturn(Optional.empty());
		given(connectionService.getActiveConnectionWithFreshToken(userId)).willReturn(Optional.of(connection));
		given(googleCalendarClient.insertCalendar("access-token", "A 프로젝트룸"))
				.willThrow(new BusinessException(ErrorCode.CALENDAR_502_001));

		RoomCalendarResponse response = projectRoomCalendarService.getRoomCalendar(userId, roomId);

		assertThat(response.connected()).isTrue();
		assertThat(response.needsReconsent()).isTrue();
		assertThat(response.calendarName()).isEqualTo("A 프로젝트룸");
		assertThat(response.googleCalendarId()).isNull();
	}

	private ProjectRoomResult room(UUID userId, UUID roomId) {
		return new ProjectRoomResult(
				roomId,
				userId,
				"A 프로젝트룸",
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE,
				null,
				Instant.parse("2026-07-05T00:00:00Z"),
				Instant.parse("2026-07-05T00:00:00Z")
		);
	}
}
