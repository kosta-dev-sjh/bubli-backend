package com.bubli.personal.calendar.repository;

import com.bubli.personal.calendar.entity.GoogleCalendarConnection;
import com.bubli.personal.calendar.entity.ProjectRoomGoogleCalendar;
import com.bubli.personal.calendar.type.GoogleCalendarConnectionStatus;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
class GoogleCalendarRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	GoogleCalendarConnectionRepository connectionRepository;

	@Autowired
	ProjectRoomGoogleCalendarRepository roomCalendarRepository;

	@Test
	void upsertActiveConnectionKeepsOneConnectionAndPreservesExistingRefreshTokenWhenMissing() {
		User user = userRepository.save(User.createGoogleUser(
				"google-sub-calendar-connection",
				"calendar-connection",
				"민서",
				null,
				"ko",
				"Asia/Seoul"
		));
		Instant firstExpiresAt = Instant.parse("2026-07-05T01:00:00Z");
		Instant secondExpiresAt = Instant.parse("2026-07-05T02:00:00Z");

		connectionRepository.upsertActiveConnection(
				UUID.randomUUID(),
				user.getId(),
				"old@example.com",
				"access-token-1",
				"refresh-token-1",
				firstExpiresAt
		);
		connectionRepository.upsertActiveConnection(
				UUID.randomUUID(),
				user.getId(),
				"new@example.com",
				"access-token-2",
				"",
				secondExpiresAt
		);

		assertThat(connectionRepository.findAll()).hasSize(1);
		GoogleCalendarConnection connection = connectionRepository.findByUserId(user.getId()).orElseThrow();
		assertThat(connection.getGoogleAccountEmail()).isEqualTo("new@example.com");
		assertThat(connection.getAccessToken()).isEqualTo("access-token-2");
		assertThat(connection.getRefreshToken()).isEqualTo("refresh-token-1");
		assertThat(connection.getExpiresAt()).isEqualTo(secondExpiresAt);
		assertThat(connection.getStatus()).isEqualTo(GoogleCalendarConnectionStatus.ACTIVE);
	}

	@Test
	void insertRoomCalendarIfAbsentKeepsOneMappingPerUserAndRoom() {
		User user = userRepository.save(User.createGoogleUser(
				"google-sub-room-calendar",
				"room-calendar",
				"정현",
				null,
				"ko",
				"Asia/Seoul"
		));
		ProjectRoom room = projectRoomRepository.save(ProjectRoom.create(
				user.getId(),
				"앱 리뉴얼",
				"클라이언트",
				null,
				null,
				null,
				null,
				null
		));

		roomCalendarRepository.lockUserRoomMapping(user.getId() + ":" + room.getId() + ":project-room-google-calendar");
		roomCalendarRepository.insertIfAbsent(
				UUID.randomUUID(),
				user.getId(),
				room.getId(),
				"google-calendar-1",
				"앱 리뉴얼"
		);
		roomCalendarRepository.insertIfAbsent(
				UUID.randomUUID(),
				user.getId(),
				room.getId(),
				"google-calendar-2",
				"겹친 캘린더"
		);

		assertThat(roomCalendarRepository.findAll()).hasSize(1);
		ProjectRoomGoogleCalendar mapping = roomCalendarRepository
				.findByUserIdAndRoomId(user.getId(), room.getId())
				.orElseThrow();
		assertThat(mapping.getGoogleCalendarId()).isEqualTo("google-calendar-1");
		assertThat(mapping.getCalendarName()).isEqualTo("앱 리뉴얼");
	}
}
