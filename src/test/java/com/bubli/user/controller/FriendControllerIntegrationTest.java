package com.bubli.user.controller;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.FriendRequest;
import com.bubli.user.entity.Friendship;
import com.bubli.user.entity.User;
import com.bubli.user.repository.FriendRequestRepository;
import com.bubli.user.repository.FriendshipRepository;
import com.bubli.user.repository.UserRepository;
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
class FriendControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	FriendRequestRepository friendRequestRepository;

	@Autowired
	FriendshipRepository friendshipRepository;

	// ── 친구 목록 ─────────────────────────────────────────────────────────────

	@Test
	void getFriendsReturnsEmptyListWhenNoFriends() throws Exception {
		User user = createUser("google-sub-friends-empty", "정현");

		mockMvc.perform(get("/api/friends")
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data", hasSize(0)))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void getFriendsReturnsAcceptedFriends() throws Exception {
		User user = createUser("google-sub-friends-list", "정현");
		User friend = createUser("google-sub-friends-list-friend", "미연");
		makeFriends(user.getId(), friend.getId());

		mockMvc.perform(get("/api/friends")
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].userId").value(friend.getId().toString()))
				.andExpect(jsonPath("$.data[0].bubliId").isNotEmpty())
				.andExpect(jsonPath("$.data[0].name").value("미연"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void getFriendsRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/friends"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_401_001"));
	}

	// ── 사용자 검색 ────────────────────────────────────────────────────────────

	@Test
	void searchByBubliIdReturnsTargetUser() throws Exception {
		User user = createUser("google-sub-search-requester", "정현");
		User target = createUser("google-sub-search-target", "미연");

		mockMvc.perform(get("/api/friends/search")
						.param("bubliId", target.getBubliId())
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(target.getId().toString()))
				.andExpect(jsonPath("$.data.name").value("미연"))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	@Test
	void searchByBubliIdReturns404ForUnknownUser() throws Exception {
		User user = createUser("google-sub-search-404", "정현");

		mockMvc.perform(get("/api/friends/search")
						.param("bubliId", "nobody-bubli-id")
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("USER_404_001"));
	}

	@Test
	void searchByBubliIdReturns400WhenSearchingSelf() throws Exception {
		User user = createUser("google-sub-search-self", "정현");

		mockMvc.perform(get("/api/friends/search")
						.param("bubliId", user.getBubliId())
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("USER_400_001"));
	}

	// ── 친구 요청 보내기 ────────────────────────────────────────────────────────

	@Test
	void sendFriendRequestPersistsAndReturnsResponse() throws Exception {
		User requester = createUser("google-sub-req-send", "정현");
		User receiver = createUser("google-sub-req-recv", "미연");

		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(requester.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "%s" }
								""".formatted(receiver.getBubliId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNotEmpty())
				.andExpect(jsonPath("$.data.requesterId").value(requester.getId().toString()))
				.andExpect(jsonPath("$.data.receiverId").value(receiver.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(friendRequestRepository.findAll()).hasSize(1);
	}

	@Test
	void sendFriendRequestToSelfReturns400() throws Exception {
		User user = createUser("google-sub-req-self", "정현");

		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "%s" }
								""".formatted(user.getBubliId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("USER_400_001"));
	}

	@Test
	void sendFriendRequestToUnknownUserReturns404() throws Exception {
		User user = createUser("google-sub-req-unknown", "정현");

		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "no-such-user" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("USER_404_001"));
	}

	@Test
	void sendDuplicateFriendRequestReturns409() throws Exception {
		User requester = createUser("google-sub-req-dup", "정현");
		User receiver = createUser("google-sub-req-dup-recv", "미연");
		friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));

		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(requester.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "%s" }
								""".formatted(receiver.getBubliId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("USER_409_001"));
	}

	@Test
	void sendFriendRequestToAlreadyFriendReturns409() throws Exception {
		User user = createUser("google-sub-req-already-friend", "정현");
		User friend = createUser("google-sub-req-already-friend-recv", "미연");
		makeFriends(user.getId(), friend.getId());

		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "%s" }
								""".formatted(friend.getBubliId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("USER_409_002"));
	}

	// ── 친구 요청 목록 ──────────────────────────────────────────────────────────

	@Test
	void getFriendRequestsReturnsSentAndReceived() throws Exception {
		User user = createUser("google-sub-req-list", "정현");
		User other1 = createUser("google-sub-req-list-a", "미연");
		User other2 = createUser("google-sub-req-list-b", "준화");
		friendRequestRepository.save(FriendRequest.create(user.getId(), other1.getId()));
		friendRequestRepository.save(FriendRequest.create(other2.getId(), user.getId()));

		mockMvc.perform(get("/api/friend-requests")
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data", hasSize(2)))
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	// ── 친구 요청 수락 ──────────────────────────────────────────────────────────

	@Test
	void acceptFriendRequestCreatesFriendshipBothDirections() throws Exception {
		User requester = createUser("google-sub-accept-req", "정현");
		User receiver = createUser("google-sub-accept-recv", "미연");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));

		mockMvc.perform(patch("/api/friend-requests/{id}/accept", req.getId())
						.header(AUTHORIZATION, bearer(receiver.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(req.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(friendshipRepository.findAll()).hasSize(2);
	}

	@Test
	void acceptFriendRequestByNonReceiverReturns404() throws Exception {
		User requester = createUser("google-sub-accept-deny-req", "정현");
		User receiver = createUser("google-sub-accept-deny-recv", "미연");
		User outsider = createUser("google-sub-accept-deny-out", "준화");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));

		mockMvc.perform(patch("/api/friend-requests/{id}/accept", req.getId())
						.header(AUTHORIZATION, bearer(outsider.getId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("USER_404_002"));
	}

	@Test
	void acceptAlreadyProcessedRequestReturns409() throws Exception {
		User requester = createUser("google-sub-accept-dup-req", "정현");
		User receiver = createUser("google-sub-accept-dup-recv", "미연");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));
		req.accept();
		friendRequestRepository.save(req);

		mockMvc.perform(patch("/api/friend-requests/{id}/accept", req.getId())
						.header(AUTHORIZATION, bearer(receiver.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("USER_409_003"));
	}

	// ── 친구 요청 거절 ──────────────────────────────────────────────────────────

	@Test
	void rejectFriendRequestChangesStatusToRejected() throws Exception {
		User requester = createUser("google-sub-reject-req", "정현");
		User receiver = createUser("google-sub-reject-recv", "미연");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));

		mockMvc.perform(patch("/api/friend-requests/{id}/reject", req.getId())
						.header(AUTHORIZATION, bearer(receiver.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(req.getId().toString()))
				.andExpect(jsonPath("$.data.status").value("REJECTED"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(friendshipRepository.findAll()).isEmpty();
	}

	@Test
	void rejectAlreadyProcessedRequestReturns409() throws Exception {
		User requester = createUser("google-sub-reject-dup-req", "정현");
		User receiver = createUser("google-sub-reject-dup-recv", "미연");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(requester.getId(), receiver.getId()));
		req.reject();
		friendRequestRepository.save(req);

		mockMvc.perform(patch("/api/friend-requests/{id}/reject", req.getId())
						.header(AUTHORIZATION, bearer(receiver.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("USER_409_003"));
	}

	// ── 친구 삭제 ──────────────────────────────────────────────────────────────

	@Test
	void deleteFriendRemovesBothDirections() throws Exception {
		User user = createUser("google-sub-delete-friend", "정현");
		User friend = createUser("google-sub-delete-friend-target", "미연");
		makeFriends(user.getId(), friend.getId());

		mockMvc.perform(delete("/api/friends/{friendUserId}", friend.getId())
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(friendshipRepository.findAll()).isEmpty();
	}

	@Test
	void deleteFriendAlsoClearsFriendRequestHistory() throws Exception {
		User user = createUser("google-sub-delete-req-clear", "정현");
		User friend = createUser("google-sub-delete-req-clear-target", "미연");
		FriendRequest req = friendRequestRepository.save(FriendRequest.create(user.getId(), friend.getId()));
		req.accept();
		friendRequestRepository.save(req);
		makeFriends(user.getId(), friend.getId());

		mockMvc.perform(delete("/api/friends/{friendUserId}", friend.getId())
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk());

		assertThat(friendshipRepository.findAll()).isEmpty();
		assertThat(friendRequestRepository.findAll()).isEmpty();

		// 친구 삭제 후 재요청 가능해야 함
		mockMvc.perform(post("/api/friend-requests")
						.header(AUTHORIZATION, bearer(user.getId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "bubliId": "%s" }
								""".formatted(friend.getBubliId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("PENDING"));
	}

	@Test
	void deleteFriendIsIdempotentForNonExistentFriendship() throws Exception {
		User user = createUser("google-sub-delete-none", "정현");
		UUID randomId = UUID.randomUUID();

		mockMvc.perform(delete("/api/friends/{friendUserId}", randomId)
						.header(AUTHORIZATION, bearer(user.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	// ── 헬퍼 ───────────────────────────────────────────────────────────────────

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

	private void makeFriends(UUID userId, UUID friendUserId) {
		friendshipRepository.save(Friendship.create(userId, friendUserId));
		friendshipRepository.save(Friendship.create(friendUserId, userId));
	}

	private String bearer(UUID userId) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(userId));
	}
}
