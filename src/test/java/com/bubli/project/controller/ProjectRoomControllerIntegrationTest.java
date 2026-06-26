package com.bubli.project.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.project.entity.Invitation;
import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.InvitationRepository;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class ProjectRoomControllerIntegrationTest extends PostgresIntegrationTestSupport {

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
	InvitationRepository invitationRepository;

	@BeforeEach
	void setUp() {
		invitationRepository.deleteAll();
		roomMemberRepository.deleteAll();
		projectRoomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void getMeReturnsCurrentUserProfile() throws Exception {
		User user = createUser("google-sub-me", "정현");

		mockMvc.perform(get("/api/me")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junghyun@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.email").value("junghyun@example.com"))
				.andExpect(jsonPath("$.data.name").value("정현"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void createProjectRoomPersistsRoomAndLeaderMember() throws Exception {
		User user = createUser("google-sub-room-create", "미연");

		mockMvc.perform(post("/api/project-rooms")
						.header(AUTHORIZATION, bearerToken(user.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "브랜드 사이트 제작",
								  "clientName": "블루클라이언트",
								  "contractAmount": 1500000,
								  "paymentStatus": "PENDING",
								  "paymentDueDate": "2026-07-10",
								  "status": "ACTIVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.name").value("브랜드 사이트 제작"))
				.andExpect(jsonPath("$.data.clientName").value("블루클라이언트"))
				.andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(projectRoomRepository.findAll()).hasSize(1);
		ProjectRoom savedRoom = projectRoomRepository.findAll().getFirst();
		assertThat(savedRoom.getCreatedByUserId()).isEqualTo(user.getId());
		assertThat(savedRoom.getContractAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_500_000));

		assertThat(roomMemberRepository.findAll()).hasSize(1);
		RoomMember leader = roomMemberRepository.findAll().getFirst();
		assertThat(leader.getRoomId()).isEqualTo(savedRoom.getId());
		assertThat(leader.getUserId()).isEqualTo(user.getId());
		assertThat(leader.getRole()).isEqualTo(RoomMemberRole.PROJECT_LEADER);
		assertThat(leader.getStatus()).isEqualTo(RoomMemberStatus.ACTIVE);
	}

	@Test
	void getProjectRoomsReturnsOnlyRoomsWithActiveMembership() throws Exception {
		User user = createUser("google-sub-room-list", "준화");
		User otherUser = createUser("google-sub-other", "재민");
		ProjectRoom activeRoom = saveRoom(user.getId(), "앱 UI 개선");
		ProjectRoom otherRoom = saveRoom(otherUser.getId(), "다른 사람 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(activeRoom.getId(), user.getId()));
		roomMemberRepository.save(RoomMember.createLeader(otherRoom.getId(), otherUser.getId()));

		mockMvc.perform(get("/api/project-rooms")
						.header(AUTHORIZATION, bearerToken(user.getId(), "junhwa@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].id").value(activeRoom.getId().toString()))
				.andExpect(jsonPath("$.data.items[0].name").value("앱 UI 개선"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void getMyProjectRoomsReturnsOnlyRoomsWithActiveMembership() throws Exception {
		User user = createUser("google-sub-my-room-list", "마렌");
		User otherUser = createUser("google-sub-my-room-other", "소민");
		ProjectRoom activeRoom = saveRoom(user.getId(), "자료 정리 프로젝트");
		ProjectRoom otherRoom = saveRoom(otherUser.getId(), "다른 팀 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(activeRoom.getId(), user.getId()));
		roomMemberRepository.save(RoomMember.createLeader(otherRoom.getId(), otherUser.getId()));

		mockMvc.perform(get("/api/me/project-rooms")
						.header(AUTHORIZATION, bearerToken(user.getId(), "maren@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].id").value(activeRoom.getId().toString()))
				.andExpect(jsonPath("$.data.items[0].name").value("자료 정리 프로젝트"))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
	void projectLeaderCanUpdateProjectRoom() throws Exception {
		User leader = createUser("google-sub-room-update-leader", "미연");
		ProjectRoom room = saveRoom(leader.getId(), "기존 프로젝트룸");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(patch("/api/project-rooms/{roomId}", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "변경된 프로젝트룸",
								  "clientName": "새 클라이언트",
								  "status": "ACTIVE"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.name").value("변경된 프로젝트룸"))
				.andExpect(jsonPath("$.data.clientName").value("새 클라이언트"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		ProjectRoom updated = projectRoomRepository.findById(room.getId()).orElseThrow();
		assertThat(updated.getName()).isEqualTo("변경된 프로젝트룸");
		assertThat(updated.getClientName()).isEqualTo("새 클라이언트");
	}

	@Test
	void ordinaryMemberCannotUpdateProjectRoom() throws Exception {
		User leader = createUser("google-sub-room-update-deny-leader", "미연");
		User member = createUser("google-sub-room-update-deny-member", "정현");
		ProjectRoom room = saveRoom(leader.getId(), "수정 거절 프로젝트룸");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(patch("/api/project-rooms/{roomId}", room.getId())
						.header(AUTHORIZATION, bearerToken(member.getId(), "junghyun@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "권한 없는 변경"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PROJECT_403_002"));
	}

	@Test
	void projectLeaderCanUpdateProjectRoomPayment() throws Exception {
		User leader = createUser("google-sub-payment-leader", "미연");
		ProjectRoom room = saveRoom(leader.getId(), "입금 관리 프로젝트룸");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(patch("/api/project-rooms/{roomId}/payment", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "contractAmount": 2300000,
								  "paymentStatus": "PAID",
								  "paymentDueDate": "2026-07-20",
								  "paidAt": "2026-07-18"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.contractAmount").value(2300000))
				.andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
				.andExpect(jsonPath("$.data.paymentDueDate").value("2026-07-20"))
				.andExpect(jsonPath("$.data.paidAt").value("2026-07-18"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		ProjectRoom updated = projectRoomRepository.findById(room.getId()).orElseThrow();
		assertThat(updated.getContractAmount()).isEqualByComparingTo(BigDecimal.valueOf(2_300_000));
		assertThat(updated.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
	}

	@Test
	void projectLeaderCanCloseProjectRoom() throws Exception {
		User leader = createUser("google-sub-room-close-leader", "미연");
		ProjectRoom room = saveRoom(leader.getId(), "종료할 프로젝트룸");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(delete("/api/project-rooms/{roomId}", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("CLOSED"))
				.andExpect(jsonPath("$.data.closedAt").isNotEmpty())
				.andExpect(jsonPath("$.error").value(nullValue()));

		ProjectRoom closed = projectRoomRepository.findById(room.getId()).orElseThrow();
		assertThat(closed.getStatus()).isEqualTo(ProjectRoomStatus.CLOSED);
		assertThat(closed.getClosedAt()).isNotNull();
	}

	@Test
	void getProjectRoomMembersReturnsActiveMembers() throws Exception {
		User leader = createUser("google-sub-member-list-leader", "미연");
		User member = createUser("google-sub-member-list-member", "정현");
		ProjectRoom room = saveRoom(leader.getId(), "멤버 목록 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(get("/api/project-rooms/{roomId}/members", room.getId())
						.header(AUTHORIZATION, bearerToken(member.getId(), "junghyun@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(2)))
				.andExpect(jsonPath("$.data.items[0].roomId").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void projectLeaderCanCreateInvitation() throws Exception {
		User leader = createUser("google-sub-invite-leader", "미연");
		User invitee = createUser("google-sub-invite-target", "준화");
		ProjectRoom room = saveRoom(leader.getId(), "초대 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(post("/api/project-rooms/{roomId}/invitations", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "inviteeUserId": "%s",
								  "role": "MEMBER"
								}
								""".formatted(invitee.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.roomId").value(room.getId().toString()))
				.andExpect(jsonPath("$.data.inviteeUserId").value(invitee.getId().toString()))
				.andExpect(jsonPath("$.data.inviteeName").value("준화"))
				.andExpect(jsonPath("$.data.role").value("MEMBER"))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(invitationRepository.findAll()).hasSize(1);
		assertThat(invitationRepository.findAll().getFirst().getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	void ordinaryMemberCannotCreateInvitation() throws Exception {
		User leader = createUser("google-sub-invite-reject-leader", "미연");
		User member = createUser("google-sub-invite-reject-member", "정현");
		User invitee = createUser("google-sub-invite-reject-target", "준화");
		ProjectRoom room = saveRoom(leader.getId(), "초대 거절 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(post("/api/project-rooms/{roomId}/invitations", room.getId())
						.header(AUTHORIZATION, bearerToken(member.getId(), "junghyun@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "inviteeUserId": "%s"
								}
								""".formatted(invitee.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PROJECT_403_002"));
	}

	@Test
	void projectLeaderCanListInvitations() throws Exception {
		User leader = createUser("google-sub-invite-list-leader", "미연");
		User invitee = createUser("google-sub-invite-list-target", "재민");
		ProjectRoom room = saveRoom(leader.getId(), "초대 목록 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		invitationRepository.save(Invitation.create(
				room.getId(),
				leader.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER,
				java.time.Instant.now().plusSeconds(3600)
		));

		mockMvc.perform(get("/api/project-rooms/{roomId}/invitations", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items", hasSize(1)))
				.andExpect(jsonPath("$.data.items[0].inviteeUserId").value(invitee.getId().toString()))
				.andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void inviteeCanAcceptInvitation() throws Exception {
		User leader = createUser("google-sub-accept-leader", "미연");
		User invitee = createUser("google-sub-accept-target", "민서");
		ProjectRoom room = saveRoom(leader.getId(), "초대 수락 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		Invitation invitation = invitationRepository.save(Invitation.create(
				room.getId(),
				leader.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER,
				java.time.Instant.now().plusSeconds(3600)
		));

		mockMvc.perform(patch("/api/invitations/{invitationId}/accept", invitation.getId())
						.header(AUTHORIZATION, bearerToken(invitee.getId(), "minseo@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(invitation.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.data.acceptedAt").isNotEmpty())
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(roomMemberRepository.findByRoomIdAndUserIdAndStatus(
				room.getId(),
				invitee.getId(),
				RoomMemberStatus.ACTIVE
		)).isPresent();
		assertThat(invitationRepository.findById(invitation.getId()).orElseThrow().getStatus())
				.isEqualTo(InvitationStatus.ACCEPTED);
	}

	@Test
	void projectLeaderCanCancelInvitation() throws Exception {
		User leader = createUser("google-sub-cancel-leader", "미연");
		User invitee = createUser("google-sub-cancel-target", "준화");
		ProjectRoom room = saveRoom(leader.getId(), "초대 취소 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		Invitation invitation = invitationRepository.save(Invitation.create(
				room.getId(),
				leader.getId(),
				invitee.getId(),
				RoomMemberRole.MEMBER,
				java.time.Instant.now().plusSeconds(3600)
		));

		mockMvc.perform(patch("/api/invitations/{invitationId}/cancel", invitation.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.status").value("CANCELED"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(invitationRepository.findById(invitation.getId()).orElseThrow().getStatus())
				.isEqualTo(InvitationStatus.CANCELED);
	}

	@Test
	void projectLeaderCanUpdateMemberRole() throws Exception {
		User leader = createUser("google-sub-role-leader", "미연");
		User member = createUser("google-sub-role-member", "정현");
		ProjectRoom room = saveRoom(leader.getId(), "역할 변경 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(patch("/api/project-rooms/{roomId}/members/{userId}", room.getId(), member.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "role": "PROJECT_LEADER"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value(member.getId().toString()))
				.andExpect(jsonPath("$.data.role").value("PROJECT_LEADER"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void projectLeaderCanRemoveMember() throws Exception {
		User leader = createUser("google-sub-remove-leader", "미연");
		User member = createUser("google-sub-remove-member", "정현");
		ProjectRoom room = saveRoom(leader.getId(), "멤버 제거 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(delete("/api/project-rooms/{roomId}/members/{userId}", room.getId(), member.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(roomMemberRepository.findByRoomIdAndUserId(room.getId(), member.getId()).orElseThrow().getStatus())
				.isEqualTo(RoomMemberStatus.REMOVED);
	}

	@Test
	void cannotInviteAlreadyActiveRoomMember() throws Exception {
		User leader = createUser("google-sub-duplicate-leader", "미연");
		User member = createUser("google-sub-duplicate-member", "정현");
		ProjectRoom room = saveRoom(leader.getId(), "중복 초대 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));
		roomMemberRepository.save(RoomMember.createMember(room.getId(), member.getId()));

		mockMvc.perform(post("/api/project-rooms/{roomId}/invitations", room.getId())
						.header(AUTHORIZATION, bearerToken(leader.getId(), "miyeon@example.com"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "inviteeUserId": "%s"
								}
								""".formatted(member.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PROJECT_409_001"));
	}

	@Test
	void getProjectRoomRejectsUserWithoutActiveMembership() throws Exception {
		User user = createUser("google-sub-no-member", "민서");
		User leader = createUser("google-sub-leader", "리더");
		ProjectRoom room = saveRoom(leader.getId(), "접근 불가 프로젝트");
		roomMemberRepository.save(RoomMember.createLeader(room.getId(), leader.getId()));

		mockMvc.perform(get("/api/project-rooms/{roomId}", room.getId())
				.header(AUTHORIZATION, bearerToken(user.getId(), "minseo@example.com")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("PROJECT_403_001"));
	}

	@Test
	void projectRoomApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/project-rooms"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_401_001"));
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
