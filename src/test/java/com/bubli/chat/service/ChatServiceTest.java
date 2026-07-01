package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.dto.ChatRoomReadResponse;
import com.bubli.chat.dto.ChatRoomResult;
import com.bubli.chat.dto.SendChatMessageCommand;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.dto.ProjectRoomResult;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
	UserPublicService userPublicService;

	@Mock
	ProjectRoomService projectRoomService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	WebSocketPublishPublicService webSocketPublishPublicService;

	@Spy
	ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	ChatService chatService;

	@Test
	void createDirectRoomCreatesRoomAndTwoMembers() {
		UUID requesterId = UUID.randomUUID();
		UUID targetUserId = UUID.randomUUID();
		UserResult targetUser = user(targetUserId, "준화");
		given(userPublicService.getUser(targetUserId)).willReturn(targetUser);
		given(chatRoomRepository.findDirectRoomBetween(
				requesterId,
				targetUserId,
				ChatType.DIRECT,
				ChatMemberStatus.ACTIVE
		)).willReturn(Optional.empty());
		given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
			ChatRoom chatRoom = invocation.getArgument(0);
			ReflectionTestUtils.setField(chatRoom, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(chatRoom, "createdAt", Instant.now());
			ReflectionTestUtils.setField(chatRoom, "updatedAt", Instant.now());
			return chatRoom;
		});

		ChatRoomResult result = chatService.createDirectRoom(requesterId, targetUserId);

		assertThat(result.chatType()).isEqualTo(ChatType.DIRECT);
		assertThat(result.name()).isEqualTo("준화");

		ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
		verify(chatRoomMemberRepository, times(2)).save(memberCaptor.capture());
		assertThat(memberCaptor.getAllValues())
				.extracting(ChatRoomMember::getUserId)
				.containsExactlyInAnyOrder(requesterId, targetUserId);
	}

	@Test
	void createDirectRoomReturnsExistingRoomWhenAlreadyExists() {
		UUID requesterId = UUID.randomUUID();
		UUID targetUserId = UUID.randomUUID();
		UserResult targetUser = user(targetUserId, "준화");
		ChatRoom existing = ChatRoom.createDirect("준화");
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(existing, "createdAt", Instant.now());
		ReflectionTestUtils.setField(existing, "updatedAt", Instant.now());
		given(userPublicService.getUser(targetUserId)).willReturn(targetUser);
		given(chatRoomRepository.findDirectRoomBetween(
				requesterId,
				targetUserId,
				ChatType.DIRECT,
				ChatMemberStatus.ACTIVE
		)).willReturn(Optional.of(existing));

		ChatRoomResult result = chatService.createDirectRoom(requesterId, targetUserId);

		assertThat(result.id()).isEqualTo(existing.getId());
		verify(chatRoomRepository, never()).save(any(ChatRoom.class));
		verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
	}

	@Test
	void createProjectRoomChatRoomCreatesRoomWithActiveProjectMembers() {
		UUID requesterId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ProjectRoomResult projectRoom = projectRoom(roomId, "프로젝트룸");
		given(projectRoomService.getProjectRoom(requesterId, roomId)).willReturn(projectRoom);
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)).willReturn(Optional.empty());
		given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
			ChatRoom chatRoom = invocation.getArgument(0);
			ReflectionTestUtils.setField(chatRoom, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(chatRoom, "createdAt", Instant.now());
			ReflectionTestUtils.setField(chatRoom, "updatedAt", Instant.now());
			return chatRoom;
		});
		given(projectMembershipPublicService.findActiveMemberIds(roomId)).willReturn(List.of(requesterId, memberId));

		ChatRoomResult result = chatService.createProjectRoomChatRoom(requesterId, roomId);

		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.chatType()).isEqualTo(ChatType.ROOM);
		assertThat(result.name()).isEqualTo("프로젝트룸");

		ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
		verify(chatRoomMemberRepository, times(2)).save(memberCaptor.capture());
		assertThat(memberCaptor.getAllValues())
				.extracting(ChatRoomMember::getUserId)
				.containsExactlyInAnyOrder(requesterId, memberId);
	}

	@Test
	void createProjectRoomChatRoomReturnsExistingRoom() {
		UUID requesterId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ProjectRoomResult projectRoom = projectRoom(roomId, "프로젝트룸");
		ChatRoom existing = ChatRoom.createRoom(roomId, "프로젝트룸");
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(existing, "createdAt", Instant.now());
		ReflectionTestUtils.setField(existing, "updatedAt", Instant.now());
		given(projectRoomService.getProjectRoom(requesterId, roomId)).willReturn(projectRoom);
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)).willReturn(Optional.of(existing));
		given(projectMembershipPublicService.findActiveMemberIds(roomId)).willReturn(List.of(requesterId));
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				existing.getId(),
				requesterId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);

		ChatRoomResult result = chatService.createProjectRoomChatRoom(requesterId, roomId);

		assertThat(result.id()).isEqualTo(existing.getId());
		verify(chatRoomRepository, never()).save(any(ChatRoom.class));
		verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
	}

	@Test
	void sendMessageStoresMessageWithNextRoomSequence() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserResult sender = user(userId, "정현");
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
		given(userPublicService.getUser(userId)).willReturn(sender);

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
		verify(webSocketPublishPublicService).publishChatMessage(result);
	}

	@Test
	void sendMessageReturnsExistingMessageWhenClientMessageIdAlreadyExists() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserResult sender = user(userId, "미연");
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
		given(userPublicService.getUser(userId)).willReturn(sender);

		ChatMessageResult result = chatService.sendMessage(userId, chatRoomId, command);

		assertThat(result.id()).isEqualTo(existing.getId());
		assertThat(result.roomSequence()).isEqualTo(3L);
		verify(chatMessageRepository, never()).save(any(ChatMessage.class));
		verify(webSocketPublishPublicService, never()).publishChatMessage(any(ChatMessageResult.class));
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
	void markReadStoresLastReadSequence() {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID messageId = UUID.randomUUID();
		long roomSequence = 1L;
		ChatRoomMember member = ChatRoomMember.create(chatRoomId, userId);
		ChatMessage message = ChatMessage.create(
				chatRoomId,
				userId,
				"client-read",
				roomSequence,
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
		given(chatMessageRepository.findByChatRoomIdAndRoomSequence(chatRoomId, roomSequence))
				.willReturn(Optional.of(message));

		ChatRoomReadResponse response = chatService.markRead(userId, chatRoomId, roomSequence);

		assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
		assertThat(response.lastReadSequence()).isEqualTo(roomSequence);
		assertThat(response.lastReadAt()).isNotNull();
		assertThat(member.getLastReadMessageId()).isEqualTo(messageId);
		assertThat(member.getLastReadSequence()).isEqualTo(roomSequence);
	}

	private ChatRoom chatRoom(UUID chatRoomId) {
		ChatRoom chatRoom = ChatRoom.createRoom(UUID.randomUUID(), "프로젝트룸 채팅");
		ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
		return chatRoom;
	}

	private UserResult user(UUID userId, String name) {
		return new UserResult(
				userId,
				"user-" + userId,
				name,
				null,
				"ko",
				"Asia/Seoul"
		);
	}

	private ProjectRoomResult projectRoom(UUID roomId, String name) {
		return new ProjectRoomResult(
				roomId,
				UUID.randomUUID(),
				name,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				Instant.now(),
				Instant.now()
		);
	}
}
