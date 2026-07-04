package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.entity.ChatMessage;
import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import com.bubli.chat.type.MessageType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.databind.JsonNode;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatMessagePublicServiceImplTest {

	@Mock
	ChatRoomRepository chatRoomRepository;

	@Mock
	ChatRoomMemberRepository chatRoomMemberRepository;

	@Mock
	ChatMessageRepository chatMessageRepository;

	@Mock
	ChatRoomAccessPublicService chatRoomAccessPublicService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	WebSocketPublishPublicService webSocketPublishPublicService;

	@Spy
	ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	ChatMessagePublicServiceImpl chatMessagePublicService;

	@Test
	void createRoomAgentResponseAddsMissingChatRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ChatRoom chatRoom = roomChat(roomId);
		JsonNode body = objectMapper.createObjectNode().put("text", "분석 결과");
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM))
				.willReturn(Optional.of(chatRoom));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId))
				.willReturn(Optional.empty());
		given(chatMessageRepository.findMaxRoomSequence(chatRoom.getId())).willReturn(4L);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> savedMessage(invocation.getArgument(0)));

		ChatMessageResult result = chatMessagePublicService.createRoomAgentResponse(userId, roomId, body, null);

		assertThat(result.roomSequence()).isEqualTo(5L);
		ArgumentCaptor<ChatRoomMember> memberCaptor = ArgumentCaptor.forClass(ChatRoomMember.class);
		verify(chatRoomMemberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getChatRoomId()).isEqualTo(chatRoom.getId());
		assertThat(memberCaptor.getValue().getUserId()).isEqualTo(userId);
		assertThat(memberCaptor.getValue().getStatus()).isEqualTo(ChatMemberStatus.ACTIVE);
	}

	@Test
	void createRoomAgentResponseReactivatesLeftMemberWithoutDuplicateSave() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ChatRoom chatRoom = roomChat(roomId);
		ChatRoomMember leftMember = ChatRoomMember.create(chatRoom.getId(), userId);
		leftMember.leave();
		JsonNode body = objectMapper.createObjectNode().put("text", "분석 결과");
		given(chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM))
				.willReturn(Optional.of(chatRoom));
		given(chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId))
				.willReturn(Optional.of(leftMember));
		given(chatMessageRepository.findMaxRoomSequence(chatRoom.getId())).willReturn(4L);
		given(chatMessageRepository.save(any(ChatMessage.class))).willAnswer(invocation -> savedMessage(invocation.getArgument(0)));

		ChatMessageResult result = chatMessagePublicService.createRoomAgentResponse(userId, roomId, body, null);

		assertThat(result.roomSequence()).isEqualTo(5L);
		assertThat(leftMember.getStatus()).isEqualTo(ChatMemberStatus.ACTIVE);
		verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
	}

	private ChatRoom roomChat(UUID roomId) {
		ChatRoom chatRoom = ChatRoom.createRoom(roomId, "프로젝트룸 채팅");
		ReflectionTestUtils.setField(chatRoom, "id", UUID.randomUUID());
		return chatRoom;
	}

	private ChatMessage savedMessage(ChatMessage message) {
		ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(message, "createdAt", Instant.now());
		assertThat(message.getMessageType()).isEqualTo(MessageType.AGENT_RESPONSE);
		return message;
	}
}
