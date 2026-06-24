package com.bubli.chat.controller;

import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
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
								  "lastReadMessageId": "%s"
								}
								""".formatted(message.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.chatRoomId").value(chatRoom.getId().toString()))
				.andExpect(jsonPath("$.data.lastReadMessageId").value(message.getId().toString()))
				.andExpect(jsonPath("$.data.lastReadAt").isNotEmpty())
				.andExpect(jsonPath("$.error").value(nullValue()));
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
