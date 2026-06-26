package com.bubli.work.schedule.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.bubli.work.schedule.entity.Schedule;
import com.bubli.work.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class ScheduleControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	RoomMemberRepository roomMemberRepository;

	@Autowired
	ScheduleRepository scheduleRepository;

	@BeforeEach
	void setUp() {
		scheduleRepository.deleteAll();
		roomMemberRepository.deleteAll();
		projectRoomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createPersonalSchedulePersistsSchedule() throws Exception {
		User user = createUser("google-sub-schedule-create", "정현");

		mockMvc.perform(post("/api/schedules")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junghyun@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "개인 포트폴리오 정리",
								  "startsAt": "2026-07-02T01:00:00Z",
								  "endsAt": "2026-07-02T02:00:00Z",
								  "allDay": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.ownerUserId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.roomId").value(nullValue()))
				.andExpect(jsonPath("$.data.title").value("개인 포트폴리오 정리"))
				.andExpect(jsonPath("$.data.syncStatus").value("LOCAL_ONLY"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(scheduleRepository.findAll()).hasSize(1);
		Schedule savedSchedule = scheduleRepository.findAll().getFirst();
		assertThat(savedSchedule.getOwnerUserId()).isEqualTo(user.getId());
		assertThat(savedSchedule.getRoomId()).isNull();
	}

	@Test
	void getSchedulesReturnsPersonalAndRoomSchedulesForActiveMember() throws Exception {
		User user = createUser("google-sub-schedule-list", "미연");
		User otherUser = createUser("google-sub-schedule-other", "준화");
		ProjectRoom room = saveRoom(user.getId(), "앱 UI 개선");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		saveSchedule(user.getId(), null, "개인 일정", "2026-07-02T01:00:00Z");
		saveSchedule(otherUser.getId(), room.getId(), "룸 일정", "2026-07-03T01:00:00Z");
		saveSchedule(otherUser.getId(), null, "다른 사람 개인 일정", "2026-07-04T01:00:00Z");

		mockMvc.perform(get("/api/schedules?page=0&size=20")
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(2)))
				.andExpect(jsonPath("$.data.items[0].title").value("개인 일정"))
				.andExpect(jsonPath("$.data.items[1].title").value("룸 일정"))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void createRoomScheduleRejectsUserWithoutActiveMembership() throws Exception {
		User user = createUser("google-sub-schedule-no-member", "민서");
		User leader = createUser("google-sub-schedule-leader", "리더");
		ProjectRoom room = saveRoom(leader.getId(), "접근 불가 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(post("/api/schedules")
						.header(AUTHORIZATION, bearerToken(user.getId(), "minseo@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "roomId": "%s",
								  "title": "접근 불가 일정",
								  "startsAt": "2026-07-03T01:00:00Z",
								  "endsAt": "2026-07-03T02:00:00Z",
								  "allDay": false
								}
								""".formatted(room.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PROJECT_403_001"));
	}

	@Test
	void updateAndDeletePersonalSchedule() throws Exception {
		User user = createUser("google-sub-schedule-update", "재민");
		Schedule schedule = saveSchedule(user.getId(), null, "기존 일정", "2026-07-02T01:00:00Z");

		mockMvc.perform(patch("/api/schedules/{scheduleId}", schedule.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "jaemin@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "수정된 일정",
								  "startsAt": "2026-07-02T03:00:00Z",
								  "endsAt": "2026-07-02T04:00:00Z",
								  "allDay": false
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.title").value("수정된 일정"));

		mockMvc.perform(delete("/api/schedules/{scheduleId}", schedule.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "jaemin@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(scheduleRepository.findById(schedule.getId())).isEmpty();
	}

	private User createUser(String googleSub, String name) {
		return userRepository.save(User.createGoogleUser(
				googleSub,
				googleSub.replace("google-sub-", ""),
				name,
				null,
				"ko",
				"Asia/Seoul"
		));
	}

	private ProjectRoom saveRoom(UUID userId, String name) {
		return projectRoomRepository.save(ProjectRoom.create(
				userId,
				name,
				null,
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		));
	}

	private Schedule saveSchedule(UUID userId, UUID roomId, String title, String startsAt) {
		return scheduleRepository.save(Schedule.create(
				userId,
				roomId,
				null,
				null,
				title,
				Instant.parse(startsAt),
				null,
				false
		));
	}

	private String bearerToken(UUID userId, String ignored) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
