package com.bubli.work.controller;

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
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.repository.WbsItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class WorkControllerIntegrationTest extends PostgresIntegrationTestSupport {

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
	WbsItemRepository wbsItemRepository;

	@Autowired
	TaskRepository taskRepository;

	@BeforeEach
	void setUp() {
		taskRepository.deleteAll();
		wbsItemRepository.deleteAll();
		roomMemberRepository.deleteAll();
		projectRoomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createAndListPersonalTask() throws Exception {
		User user = createUser("google-sub-personal-task", "정현");

		mockMvc.perform(post("/api/tasks")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junghyun@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "오늘 정리",
								  "description": "회의 내용 정리",
								  "status": "TODO"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.ownerUserId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.roomId").value(nullValue()))
				.andExpect(jsonPath("$.data.title").value("오늘 정리"));

		mockMvc.perform(get("/api/tasks")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junghyun@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].title").value("오늘 정리"));
	}

	@Test
	void createWbsAndRoomTaskForActiveRoomMember() throws Exception {
		User user = createUser("google-sub-work-task", "미연");
		ProjectRoom room = saveRoom(user.getId(), "브랜드 사이트 제작");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));

		String wbsResponse = mockMvc.perform(post("/api/project-rooms/{roomId}/wbs-items", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "요구사항 정리",
								  "status": "TODO"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.roomId").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.orderNo").value(1))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID wbsItemId = UUID.fromString(wbsResponse.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1"));

		mockMvc.perform(post("/api/project-rooms/{roomId}/tasks", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "assigneeUserId": "%s",
								  "wbsItemId": "%s",
								  "title": "계약서 체크",
								  "status": "IN_PROGRESS"
								}
								""".formatted(user.getId(), wbsItemId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.roomId").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.assigneeUserId").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.wbsItemId").value(wbsItemId.toString()))
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

		mockMvc.perform(get("/api/project-rooms/{roomId}/tasks", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].title").value("계약서 체크"));
	}

	@Test
	void roomTaskApiRejectsUserWithoutActiveMembership() throws Exception {
		User user = createUser("google-sub-work-no-member", "민서");
		User leader = createUser("google-sub-work-leader", "리더");
		ProjectRoom room = saveRoom(leader.getId(), "접근 불가 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(get("/api/project-rooms/{roomId}/tasks", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId(), "minseo@example.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("PROJECT_403_001"));
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

	private String bearerToken(UUID userId, String email) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId, email));
	}
}
