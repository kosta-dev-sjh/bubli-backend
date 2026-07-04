package com.bubli.work.schedule.repository;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.bubli.work.schedule.entity.Schedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
class ScheduleRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	ScheduleRepository scheduleRepository;

	@Test
	void visibleScheduleQueryReturnsPersonalAndActiveRoomSchedulesOnly() {
		User user = saveUser("schedule-visible-user", "schedule-visible");
		User otherUser = saveUser("schedule-visible-other-user", "schedule-visible-other");
		ProjectRoom activeRoom = saveRoom(user, "활성 룸");
		ProjectRoom leftRoom = saveRoom(user, "나간 룸");
		Instant from = Instant.parse("2026-07-10T00:00:00Z");
		Instant to = Instant.parse("2026-07-11T00:00:00Z");

		scheduleRepository.saveAll(List.of(
				Schedule.create(
						user.getId(),
						null,
						null,
						null,
						"개인 일정",
						Instant.parse("2026-07-10T01:00:00Z"),
						Instant.parse("2026-07-10T02:00:00Z"),
						false
				),
				Schedule.create(
						user.getId(),
						null,
						null,
						null,
						"전날 시작해 겹치는 개인 일정",
						Instant.parse("2026-07-09T23:00:00Z"),
						Instant.parse("2026-07-10T01:00:00Z"),
						false
				),
				Schedule.create(
						otherUser.getId(),
						activeRoom.getId(),
						null,
						null,
						"활성 룸 일정",
						Instant.parse("2026-07-10T03:00:00Z"),
						Instant.parse("2026-07-10T04:00:00Z"),
						false
				),
				Schedule.create(
						otherUser.getId(),
						activeRoom.getId(),
						null,
						null,
						"전날 시작해 겹치는 룸 일정",
						Instant.parse("2026-07-09T22:00:00Z"),
						Instant.parse("2026-07-10T00:30:00Z"),
						false
				),
				Schedule.create(
						user.getId(),
						leftRoom.getId(),
						null,
						null,
						"나간 룸 일정",
						Instant.parse("2026-07-10T05:00:00Z"),
						Instant.parse("2026-07-10T06:00:00Z"),
						false
				),
				Schedule.create(
						otherUser.getId(),
						null,
						null,
						null,
						"다른 사람 개인 일정",
						Instant.parse("2026-07-10T07:00:00Z"),
						Instant.parse("2026-07-10T08:00:00Z"),
						false
				),
				Schedule.create(
						user.getId(),
						null,
						null,
						null,
						"다음 날 경계 일정",
						to,
						Instant.parse("2026-07-11T01:00:00Z"),
						false
				),
				Schedule.create(
						user.getId(),
						null,
						null,
						null,
						"전날 끝난 일정",
						Instant.parse("2026-07-09T22:00:00Z"),
						from,
						false
				),
				Schedule.create(
						user.getId(),
						null,
						null,
						null,
						"종료 없는 과거 일정",
						Instant.parse("2026-07-09T22:00:00Z"),
						null,
						false
				)
		));

		assertThat(scheduleRepository.findVisibleBetweenForUser(user.getId(), List.of(activeRoom.getId()), from, to))
				.extracting(Schedule::getTitle)
				.containsExactly("전날 시작해 겹치는 룸 일정", "전날 시작해 겹치는 개인 일정", "개인 일정", "활성 룸 일정");
		assertThat(scheduleRepository.findPersonalBetweenForUser(user.getId(), from, to))
				.extracting(Schedule::getTitle)
				.containsExactly("전날 시작해 겹치는 개인 일정", "개인 일정");
		assertThat(scheduleRepository.findRoomOverlappingForRoom(activeRoom.getId(), from, to))
				.extracting(Schedule::getTitle)
				.containsExactly("전날 시작해 겹치는 룸 일정", "활성 룸 일정");
	}

	private User saveUser(String googleSub, String bubliId) {
		return userRepository.save(User.createGoogleUser(
				googleSub,
				bubliId,
				"민서",
				null,
				"ko",
				"Asia/Seoul"
		));
	}

	private ProjectRoom saveRoom(User user, String name) {
		return projectRoomRepository.save(ProjectRoom.create(
				user.getId(),
				name,
				null,
				null,
				null,
				null,
				null,
				null
		));
	}
}
