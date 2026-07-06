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
import com.bubli.project.service.ProjectRoomPublicService;
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
import org.springframework.dao.DataIntegrityViolationException;
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
	ProjectRoomPublicService projectRoomPublicService;

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
		given(projectRoomPublicService.getProjectRoom(requesterId, roomId)).willReturn(projectRoom);
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)).willReturn(Optional.empty());
		given(chatRoomRepository.save(any(ChatRoom.class))).willAnswer(invocation -> {
			ChatRoom chatRoom = invocation.getArgument(0);
			ReflectionTestUtils.setField(chatRoom, "id", UUID.randomUUID());
			ReflectionTestUtils.setField(chatRoom, "createdAt", Instant.now());
			ReflectionTestUtils.setField(chatRoom, "updatedAt", Instant.now());
			return chatRoom;
		});
		given(projectMembershipPublicService.findActiveMemberIds(roomId)).willReturn(List.of(requesterId, memberId));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdIn(any(UUID.class), any()))
				.willReturn(List.of());

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
		given(projectRoomPublicService.getProjectRoom(requesterId, roomId)).willReturn(projectRoom);
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)).willReturn(Optional.of(existing));
		given(projectMembershipPublicService.findActiveMemberIds(roomId)).willReturn(List.of(requesterId));
		ChatRoomMember existingMember = ChatRoomMember.create(existing.getId(), requesterId);
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdIn(
				existing.getId(),
				List.of(requesterId)
		)).willReturn(List.of(existingMember));

		ChatRoomResult result = chatService.createProjectRoomChatRoom(requesterId, roomId);

		assertThat(result.id()).isEqualTo(existing.getId());
		verify(chatRoomRepository, never()).save(any(ChatRoom.class));
		verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
	}

	@Test
	void createProjectRoomChatRoomReactivatesLeftChatMemberWithoutDuplicateSave() {
		UUID requesterId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ProjectRoomResult projectRoom = projectRoom(roomId, "프로젝트룸");
		ChatRoom existing = ChatRoom.createRoom(roomId, "프로젝트룸");
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		ChatRoomMember leftMember = ChatRoomMember.create(existing.getId(), memberId);
		leftMember.leave();
		given(projectRoomPublicService.getProjectRoom(requesterId, roomId)).willReturn(projectRoom);
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)).willReturn(Optional.of(existing));
		given(projectMembershipPublicService.findActiveMemberIds(roomId)).willReturn(List.of(memberId));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdIn(existing.getId(), List.of(memberId)))
				.willReturn(List.of(leftMember));

		chatService.createProjectRoomChatRoom(requesterId, roomId);

		assertThat(leftMember.getStatus()).isEqualTo(ChatMemberStatus.ACTIVE);
		verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
	}

	@Test
	void inviteMembersSavesMissingMember() {
		UUID inviterId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ChatRoom chatRoom = ChatRoom.createGroup("그룹 채팅");
		ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				inviterId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
		given(userPublicService.getUser(memberId)).willReturn(user(memberId, "민서"));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdIn(chatRoomId, List.of(memberId)))
				.willReturn(List.of());

		chatService.inviteMembers(inviterId, chatRoomId, List.of(memberId));

		ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
		verify(chatRoomMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getChatRoomId()).isEqualTo(chatRoomId);
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(memberId);
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(ChatMemberStatus.ACTIVE);
	}

	@Test
	void inviteMembersReactivatesLeftMemberWithoutDuplicateSave() {
		UUID inviterId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		ChatRoom chatRoom = ChatRoom.createGroup("그룹 채팅");
		ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);
		ChatRoomMember leftMember = ChatRoomMember.create(chatRoomId, memberId);
		leftMember.leave();
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				inviterId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
		given(userPublicService.getUser(memberId)).willReturn(user(memberId, "민서"));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdIn(chatRoomId, List.of(memberId)))
				.willReturn(List.of(leftMember));

		chatService.inviteMembers(inviterId, chatRoomId, List.of(memberId));

		assertThat(leftMember.getStatus()).isEqualTo(ChatMemberStatus.ACTIVE);
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
		given(chatMessageRepository.saveAndFlush(any(ChatMessage.class))).willAnswer(invocation -> {
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
		verify(chatMessageRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getClientMessageId()).isEqualTo("client-1");
		assertThat(captor.getValue().getRoomSequence()).isEqualTo(8L);
		verify(webSocketPublishPublicService).publishChatMessage(result);
	}

	@Test
	void sendMessageRetriesWhenRoomSequenceConflicts() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserResult sender = user(userId, "정현");
		ChatRoom chatRoom = chatRoom(chatRoomId);
		SendChatMessageCommand command = new SendChatMessageCommand(
				"client-retry",
				MessageType.TEXT,
				objectMapper.readTree("""
						{"text":"동시 메시지"}
						"""),
				null
		);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatMessageRepository.findByChatRoomIdAndClientMessageId(chatRoomId, "client-retry"))
				.willReturn(Optional.empty(), Optional.empty());
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
		given(chatMessageRepository.findMaxRoomSequence(chatRoomId)).willReturn(7L, 8L);
		given(chatMessageRepository.saveAndFlush(any(ChatMessage.class)))
				.willThrow(new DataIntegrityViolationException("duplicate room sequence"))
				.willAnswer(invocation -> {
					ChatMessage message = invocation.getArgument(0);
					ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
					ReflectionTestUtils.setField(message, "createdAt", Instant.now());
					return message;
				});
		given(userPublicService.getUser(userId)).willReturn(sender);

		ChatMessageResult result = chatService.sendMessage(userId, chatRoomId, command);

		assertThat(result.roomSequence()).isEqualTo(9L);
		verify(chatMessageRepository, times(2)).saveAndFlush(any(ChatMessage.class));
		verify(webSocketPublishPublicService).publishChatMessage(result);
	}

	@Test
	void sendMessageReturnsExistingMessageWhenConcurrentClientMessageIdWasSaved() throws Exception {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UserResult sender = user(userId, "미연");
		ChatRoom chatRoom = chatRoom(chatRoomId);
		ChatMessage existing = ChatMessage.create(
				chatRoomId,
				userId,
				"client-race",
				5L,
				MessageType.TEXT,
				"{\"text\":\"동시에 저장됨\"}",
				null
		);
		ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(existing, "createdAt", Instant.now());
		SendChatMessageCommand command = new SendChatMessageCommand(
				"client-race",
				MessageType.TEXT,
				objectMapper.readTree("""
						{"text":"동시에 저장됨"}
						"""),
				null
		);
		given(chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(true);
		given(chatMessageRepository.findByChatRoomIdAndClientMessageId(chatRoomId, "client-race"))
				.willReturn(Optional.empty(), Optional.of(existing));
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
		given(chatMessageRepository.findMaxRoomSequence(chatRoomId)).willReturn(4L);
		given(chatMessageRepository.saveAndFlush(any(ChatMessage.class)))
				.willThrow(new DataIntegrityViolationException("duplicate client message id"));
		given(userPublicService.getUser(userId)).willReturn(sender);

		ChatMessageResult result = chatService.sendMessage(userId, chatRoomId, command);

		assertThat(result.id()).isEqualTo(existing.getId());
		assertThat(result.roomSequence()).isEqualTo(5L);
		verify(chatMessageRepository).saveAndFlush(any(ChatMessage.class));
		verify(webSocketPublishPublicService, never()).publishChatMessage(any(ChatMessageResult.class));
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

	@Test
	void leaveRoomMarksMemberAsLeft() {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		ChatRoom directRoom = ChatRoom.createDirect("상대방");
		ReflectionTestUtils.setField(directRoom, "id", chatRoomId);
		ChatRoomMember member = ChatRoomMember.create(chatRoomId, userId);
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(directRoom));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(Optional.of(member));

		chatService.leaveRoom(userId, chatRoomId);

		assertThat(member.getStatus()).isEqualTo(ChatMemberStatus.LEFT);
	}

	@Test
	void leaveRoomRejectsProjectRoomChat() {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom(chatRoomId)));

		assertThatThrownBy(() -> chatService.leaveRoom(userId, chatRoomId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.COMMON_400_002);

		verify(chatRoomMemberRepository, never())
				.findByChatRoomIdAndUserIdAndStatus(any(), any(), any());
	}

	@Test
	void leaveRoomRejectsNonMember() {
		UUID chatRoomId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		ChatRoom directRoom = ChatRoom.createDirect("상대방");
		ReflectionTestUtils.setField(directRoom, "id", chatRoomId);
		given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(directRoom));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
				chatRoomId,
				userId,
				ChatMemberStatus.ACTIVE
		)).willReturn(Optional.empty());

		assertThatThrownBy(() -> chatService.leaveRoom(userId, chatRoomId))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CHAT_403_001);
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
