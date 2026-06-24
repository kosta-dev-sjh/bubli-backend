package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.dto.ChatRoomReadResponse;
import com.bubli.chat.dto.SendChatMessageCommand;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.MessageType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

	@Mock
	ChatRoomRepository chatRoomRepository;

	@Mock
	ChatRoomMemberRepository chatRoomMemberRepository;

	@Mock
	ChatMessageRepository chatMessageRepository;

	@Mock
	UserRepository userRepository;

	@Spy
	ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	ChatService chatService;

	@Test
	void sendMessageStoresMessageWithNextRoomSequence() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		User sender = user(userId, "정현");
		ChatRoom chatRoom = chatRoom(chatRoomId);
		SendChatMessageCommand command = new SendChatMessageCommand(
				"client-1",
				MessageType.TEXT,
				objectMapper.readTree("""
						{"text":"안녕하세요"}
						"""),
				null
		);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatMessageRepository.findByChatRoomIdAndClientMessageId(chatRoomId, "client-1"))
				.willReturn(Optional.empty());
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
		given(chatMessageRepository.findMaxRoomSequence(chatRoomId)).willReturn(7L);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> {
			ChatMessage message = invocation.getArgument(0);
			ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(message, "createdAt", Instant.now());
			return message;
		});
		given(userRepository.findById(userId)).willReturn(Optional.of(sender));

		ChatMessageResult result = chatService.sendMessage(userId, chatRoomId, command);

		assertThat(result.chatRoomId()).isEqualTo(chatRoomId);
		assertThat(result.senderId()).isEqualTo(userId);
		assertThat(result.senderName()).isEqualTo("정현");
		assertThat(result.roomSequence()).isEqualTo(8L);
		assertThat(result.body().get("text").asText()).isEqualTo("안녕하세요");

		ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
		verify(chatMessageRepository).save(captor.capture());
		assertThat(captor.getValue().getClientMessageId()).isEqualTo("client-1");
		assertThat(captor.getValue().getRoomSequence()).isEqualTo(8L);
	}

	@Test
	void sendMessageReturnsExistingMessageWhenClientMessageIdAlreadyExists() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		User sender = user(userId, "미연");
		ChatMessage existing = ChatMessage.create(
				chatRoomId,
				userId,
				"client-dup",
				3L,
				MessageType.TEXT,
				"{\"text\":\"이미 보낸 메시지\"}",
				null
		);
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(existing, "createdAt", Instant.now());
		SendChatMessageCommand command = new SendChatMessageCommand(
				"client-dup",
				MessageType.TEXT,
				objectMapper.readTree("""
						{"text":"이미 보낸 메시지"}
						"""),
				null
		);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatMessageRepository.findByChatRoomIdAndClientMessageId(chatRoomId, "client-dup"))
				.willReturn(Optional.of(existing));
		given(userRepository.findById(userId)).willReturn(Optional.of(sender));

		ChatMessageResult result = chatService.sendMessage(userId, chatRoomId, command);

		assertThat(result.id()).isEqualTo(existing.getId());
		assertThat(result.roomSequence()).isEqualTo(3L);
		verify(chatMessageRepository, never()).save(any(ChatMessage.class));
	}

	@Test
	void sendMessageRequiresActiveChatRoomMember() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		SendChatMessageCommand command = new SendChatMessageCommand(
				"client-forbidden",
				MessageType.TEXT,
				objectMapper.readTree("""
						{"text":"권한 없음"}
						"""),
				null
		);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(false);

		assertThatThrownBy(() -> chatService.sendMessage(userId, chatRoomId, command))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHAT_403_001));
	}

	@Test
	void markReadStoresLastReadMessage() {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		ChatRoomMember member = ChatRoomMember.create(chatRoomId, userId);
		ChatMessage message = ChatMessage.create(
				chatRoomId,
				userId,
				"client-read",
				1L,
				MessageType.TEXT,
				"{\"text\":\"읽음\"}",
				null
		);
		ReflectionTestUtils.setField(message, "id", messageId);
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(Optional.of(member));
		given(chatMessageRepository.findByIdAndChatRoomId(messageId, chatRoomId)).willReturn(Optional.of(message));

		ChatRoomReadResponse response = chatService.markRead(userId, chatRoomId, messageId);

		assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
		assertThat(response.lastReadMessageId()).isEqualTo(messageId);
		assertThat(response.lastReadAt()).isNotNull();
		assertThat(member.getLastReadMessageId()).isEqualTo(messageId);
	}

	private ChatRoom chatRoom(UUID chatRoomId) {
		ChatRoom chatRoom = ChatRoom.createRoom(UUID.randomUUID(), "프로젝트룸 채팅");
		ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
		return chatRoom;
	}

	private User user(UUID userId, String name) {
		User user = User.createGoogleUser(
				"google-sub-" + userId,
				"user-" + userId,
				name,
				null,
				"ko",
				"Asia/Seoul"
		);
		ReflectionTestUtils.setField(user, "id", userId);
		return user;
	}
}
