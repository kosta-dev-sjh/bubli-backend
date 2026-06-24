package com.bubli.chat.controller;

import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
class ChatControllerIntegrationTest extends PostgresIntegrationTestSupport {

	private static final String AUTHORIZATION = "Authorization";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	JwtTokenProvider jwtTokenProvider;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ChatRoomRepository chatRoomRepository;

	@Autowired
	ChatRoomMemberRepository chatRoomMemberRepository;

	@Autowired
	ChatMessageRepository chatMessageRepository;

	@BeforeEach
	void setUp() {
		chatMessageRepository.deleteAll();
		chatRoomMemberRepository.deleteAll();
		chatRoomRepository.deleteAll();
		userRepository.deleteAll();
	}

	@Test
	void createDirectRoomCreatesChatRoomForTwoUsers() throws Exception {
		User requester = createUser("google-sub-direct-requester", "정현");
		User target = createUser("google-sub-direct-target", "준화");

		mockMvc.perform(post("/api/chat/direct-rooms")
						.header(AUTHORIZATION, bearerToken(requester))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "targetUserId": "%s"
								}
								""".formatted(target.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.chatType").value("DIRECT"))
				.andExpect(jsonPath("$.data.name").value("준화"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		ChatRoom chatRoom = chatRoomRepository.findAll().getFirst();
		assertThat(chatRoom.getChatType()).isEqualTo(ChatType.DIRECT);
		assertThat(chatRoomMemberRepository.findAll())
				.extracting(ChatRoomMember::getUserId)
				.containsExactlyInAnyOrder(requester.getId(), target.getId());
	}

	@Test
	void sendMessagePersistsChatMessage() throws Exception {
		User user = createUser("google-sub-chat-send", "정현");
		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createRoom(null, "프로젝트룸 채팅"));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), user.getId()));

		mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/messages", chatRoom.getId())
						.header(AUTHORIZATION, bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "clientMessageId": "client-message-1",
								  "messageType": "TEXT",
								  "body": {
								    "text": "안녕하세요"
								  }
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId().toString()))
				.andExpect(jsonPath("$.data.sender.id").value(user.getId().toString()))
				.andExpect(jsonPath("$.data.sender.name").value("정현"))
				.andExpect(jsonPath("$.data.roomSequence").value(1))
				.andExpect(jsonPath("$.data.body.text").value("안녕하세요"))
				.andExpect(jsonPath("$.error").value(nullValue()));

		assertThat(chatMessageRepository.findAll()).hasSize(1);
		assertThat(chatMessageRepository.findAll().getFirst().getClientMessageId()).isEqualTo("client-message-1");
	}

	@Test
	void sendMessageAllowsSameClientMessageIdInDifferentRooms() throws Exception {
		User user = createUser("google-sub-chat-same-client-message", "정현");
		ChatRoom firstRoom = chatRoomRepository.save(ChatRoom.createRoom(null, "첫 번째 채팅"));
		ChatRoom secondRoom = chatRoomRepository.save(ChatRoom.createRoom(null, "두 번째 채팅"));
		chatRoomMemberRepository.save(ChatRoomMember.create(firstRoom.getId(), user.getId()));
		chatRoomMemberRepository.save(ChatRoomMember.create(secondRoom.getId(), user.getId()));

		sendTextMessage(user, firstRoom, "same-client-message-id", "첫 번째 방");
		sendTextMessage(user, secondRoom, "same-client-message-id", "두 번째 방");

		assertThat(chatMessageRepository.findAll()).hasSize(2);
	}

	@Test
	void getMessagesRequiresActiveChatRoomMember() throws Exception {
		User user = createUser("google-sub-chat-forbidden", "민서");
		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createRoom(null, "접근 불가 채팅"));

		mockMvc.perform(get("/api/chat/rooms/{chatRoomId}/messages", chatRoom.getId())
						.header(AUTHORIZATION, bearerToken(user)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").value(nullValue()))
				.andExpect(jsonPath("$.error.code").value("CHAT_403_001"));
	}

	@Test
	void markReadStoresReadMessage() throws Exception {
		User user = createUser("google-sub-chat-read", "미연");
		ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createRoom(null, "읽음 채팅"));
		chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), user.getId()));
		ChatMessage message = chatMessageRepository.save(ChatMessage.create(
				chatRoom.getId(),
				user.getId(),
				"client-message-read",
				1L,
				MessageType.TEXT,
				"{\"text\":\"읽어주세요\"}",
				null
		));

		mockMvc.perform(patch("/api/chat/rooms/{chatRoomId}/read", chatRoom.getId())
						.header(AUTHORIZATION, bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "lastReadSequence": 1
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId().toString()))
				.andExpect(jsonPath("$.data.lastReadSequence").value(1))
				.andExpect(jsonPath("$.data.lastReadAt").isNotEmpty())
				.andExpect(jsonPath("$.error").value(nullValue()));
	}

	private void sendTextMessage(User user, ChatRoom chatRoom, String clientMessageId, String text) throws Exception {
		mockMvc.perform(post("/api/chat/rooms/{chatRoomId}/messages", chatRoom.getId())
						.header(AUTHORIZATION, bearerToken(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "clientMessageId": "%s",
								  "messageType": "TEXT",
								  "body": {
								    "text": "%s"
								  }
								}
								""".formatted(clientMessageId, text)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.clientMessageId").value(clientMessageId))
				.andExpect(jsonPath("$.data.roomSequence").value(1));
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

	private String bearerToken(User user) {
		return "Bearer " + jwtTokenProvider.createAccessToken(new AuthUser(user.getId(), user.getBubliId()));
	}
}
