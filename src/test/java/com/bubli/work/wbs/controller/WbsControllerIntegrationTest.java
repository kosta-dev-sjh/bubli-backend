package com.bubli.work.wbs.controller;

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
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class WbsControllerIntegrationTest extends PostgresIntegrationTestSupport {

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

	@BeforeEach
	void setUp() {
		wbsItemRepository.deleteAll();
		roomMemberRepository.deleteAll();
		projectRoomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void getRoomWbsItemsReturnsItems() throws Exception {
		User user = createUser("google-sub-wbs-list", "정현");
		ProjectRoom room = saveRoom(user.getId(), "WBS 목록 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		wbsItemRepository.save(WbsItem.create(room.getId(), null, "기능 설계", 1, WbsStatus.TODO));
		wbsItemRepository.save(WbsItem.create(room.getId(), null, "UI 구현", 2, WbsStatus.TODO));

		mockMvc.perform(get("/api/project-rooms/{roomId}/wbs-items", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(2)))
				.andExpect(jsonPath("$.data.items[0].title").value("기능 설계"))
				.andExpect(jsonPath("$.data.items[1].title").value("UI 구현"))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void createWbsItemPersistsItem() throws Exception {
		User user = createUser("google-sub-wbs-create", "미연");
		ProjectRoom room = saveRoom(user.getId(), "WBS 생성 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));

		mockMvc.perform(post("/api/project-rooms/{roomId}/wbs-items", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "요구사항 정리",
								  "status": "TODO"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.roomId").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.title").value("요구사항 정리"))
				.andExpect(jsonPath("$.data.status").value("TODO"))
				.andExpect(jsonPath("$.data.orderNo").value(1))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(wbsItemRepository.findAll()).hasSize(1);
	}

	@Test
	void updateWbsItemChangesTitle() throws Exception {
		User user = createUser("google-sub-wbs-update", "준화");
		ProjectRoom room = saveRoom(user.getId(), "WBS 수정 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		WbsItem item = wbsItemRepository.save(WbsItem.create(room.getId(), null, "기존 항목", 1, WbsStatus.TODO));

		mockMvc.perform(patch("/api/wbs-items/{wbsItemId}", item.getId())
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "수정된 항목",
								  "status": "IN_PROGRESS"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(item.getId().toString()))
				.andExpect(jsonPath("$.data.title").value("수정된 항목"))
				.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void deleteWbsItemRemovesItem() throws Exception {
		User user = createUser("google-sub-wbs-delete", "재민");
		ProjectRoom room = saveRoom(user.getId(), "WBS 삭제 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		WbsItem item = wbsItemRepository.save(WbsItem.create(room.getId(), null, "삭제할 항목", 1, WbsStatus.TODO));

		mockMvc.perform(delete("/api/wbs-items/{wbsItemId}", item.getId())
						.header(AUTHORIZATION, bearerToken(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(wbsItemRepository.findById(item.getId())).isEmpty();
	}

	@Test
	void reorderWbsItemsChangesOrder() throws Exception {
		User user = createUser("google-sub-wbs-reorder", "민서");
		ProjectRoom room = saveRoom(user.getId(), "WBS 순서 변경 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), user.getId()));
		WbsItem item1 = wbsItemRepository.save(WbsItem.create(room.getId(), null, "첫 번째 항목", 1, WbsStatus.TODO));
		WbsItem item2 = wbsItemRepository.save(WbsItem.create(room.getId(), null, "두 번째 항목", 2, WbsStatus.TODO));

		mockMvc.perform(patch("/api/project-rooms/{roomId}/wbs-items/reorder", room.getId())
						.header(AUTHORIZATION, bearerToken(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "items": [
								    {"wbsItemId": "%s", "orderNo": 2},
								    {"wbsItemId": "%s", "orderNo": 1}
								  ]
								}
								""".formatted(item1.getId(), item2.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data[0].title").value("두 번째 항목"))
				.andExpect(jsonPath("$.data[0].orderNo").value(1))
				.andExpect(jsonPath("$.data[1].title").value("첫 번째 항목"))
				.andExpect(jsonPath("$.data[1].orderNo").value(2))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void getRoomWbsItemsRejectsUserWithoutActiveMembership() throws Exception {
		User leader = createUser("google-sub-wbs-leader", "리더");
		User outsider = createUser("google-sub-wbs-outsider", "외부인");
		ProjectRoom room = saveRoom(leader.getId(), "접근 불가 WBS 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(get("/api/project-rooms/{roomId}/wbs-items", room.getId())
						.header(AUTHORIZATION, bearerToken(outsider.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
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

	private String bearerToken(UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
