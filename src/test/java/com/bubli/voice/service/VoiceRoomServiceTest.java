package com.bubli.voice.service;

import com.bubli.chat.service.ChatRoomAccessPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.project.service.ProjectRoomAccessPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.voice.config.LiveKitProperties;
import com.bubli.voice.dto.VoiceParticipantResponse;
import com.bubli.voice.dto.VoiceRoomResponse;
import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.repository.VoiceParticipantRepository;
import com.bubli.voice.repository.VoiceRoomRepository;
import com.bubli.voice.type.VoiceParticipantStatus;
import com.bubli.voice.type.VoiceRoomStatus;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
class VoiceRoomServiceTest {

	@Mock
	VoiceRoomRepository voiceRoomRepository;

	@Mock
	VoiceParticipantRepository voiceParticipantRepository;

	@Mock
	ProjectRoomAccessPublicService projectRoomAccessPublicService;

	@Mock
	ProjectRoomEventPublicService projectRoomEventPublicService;

	@Mock
	ChatRoomAccessPublicService chatRoomAccessPublicService;

	@Mock
	UserPublicService userPublicService;

	@Mock
	NotificationPublicService notificationPublicService;

	@Mock
	WebSocketPublishPublicService webSocketPublishPublicService;

	VoiceRoomService voiceRoomService;

	@BeforeEach
	void setUp() {
		voiceRoomService = new VoiceRoomService(
				voiceRoomRepository,
				voiceParticipantRepository,
				projectRoomAccessPublicService,
				projectRoomEventPublicService,
				chatRoomAccessPublicService,
				userPublicService,
				notificationPublicService,
				new LiveKitProperties("api-key", "test-secret-key-must-be-at-least-32-bytes", "wss://livekit.example"),
				webSocketPublishPublicService
		);
	}

	@Test
	void createVoiceRoomRecordsRoomAndParticipantEvents() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN)).willReturn(Optional.empty());
		given(voiceRoomRepository.save(any(VoiceRoom.class))).willAnswer(invocation -> withId((VoiceRoom) invocation.getArgument(0)));
		given(voiceParticipantRepository.save(any(VoiceParticipant.class)))
				.willAnswer(invocation -> withId((VoiceParticipant) invocation.getArgument(0)));
		given(userPublicService.getUser(userId)).willReturn(user(userId));

		VoiceRoomResponse response = voiceRoomService.createVoiceRoom(userId, roomId, null);

		assertThat(response.roomId()).isEqualTo(roomId);
		verify(projectRoomAccessPublicService).requireRoomMember(roomId, userId);
		verify(voiceRoomRepository).lockRoomOpenCreation("voice-room-open:" + roomId);
		verify(projectRoomEventPublicService).recordVoiceRoomCreated(
				userId,
				roomId,
				response.id(),
				response.livekitRoomName()
		);
		verify(projectRoomEventPublicService).recordVoiceParticipantJoined(
				userId,
				roomId,
				response.id(),
				response.participants().getFirst().id(),
				userId
		);
	}

	@Test
	void createVoiceRoomReturnsExistingOpenRoomAfterLock() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN))
				.willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(participant));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(userId, user(userId)));

		VoiceRoomResponse response = voiceRoomService.createVoiceRoom(userId, roomId, null);

		assertThat(response.id()).isEqualTo(voiceRoom.getId());
		verify(projectRoomAccessPublicService).requireRoomMember(roomId, userId);
		verify(voiceRoomRepository).lockRoomOpenCreation("voice-room-open:" + roomId);
		verify(voiceRoomRepository, never()).save(any(VoiceRoom.class));
		verify(projectRoomEventPublicService, never()).recordVoiceRoomCreated(
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	void createChatVoiceRoomNotifiesOtherActiveMembersOnCreate() {
		UUID callerId = UUID.randomUUID();
		UUID otherMemberId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		given(voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN)).willReturn(Optional.empty());
		given(voiceRoomRepository.save(any(VoiceRoom.class))).willAnswer(invocation -> withId((VoiceRoom) invocation.getArgument(0)));
		given(voiceParticipantRepository.save(any(VoiceParticipant.class)))
				.willAnswer(invocation -> withId((VoiceParticipant) invocation.getArgument(0)));
		given(userPublicService.getUser(callerId)).willReturn(user(callerId, "미연"));
		given(chatRoomAccessPublicService.findActiveMemberIds(chatRoomId)).willReturn(List.of(callerId, otherMemberId));

		voiceRoomService.createVoiceRoom(callerId, null, chatRoomId);

		verify(chatRoomAccessPublicService).assertActiveMember(callerId, chatRoomId);
		verify(notificationPublicService).create(
				otherMemberId,
				com.bubli.personal.notification.type.NotificationSourceType.VOICE_CALL,
				chatRoomId,
				"미연",
				"보이스 통화를 시작했습니다"
		);
		verify(notificationPublicService, never()).create(
				org.mockito.ArgumentMatchers.eq(callerId),
				any(),
				any(),
				any(),
				any()
		);
	}

	@Test
	void createChatVoiceRoomReusesExistingOpenRoomWithoutNotifying() {
		UUID callerId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		VoiceRoom existing = withId(VoiceRoom.createForChatRoom(chatRoomId, callerId));
		given(voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN)).willReturn(Optional.of(existing));
		given(voiceParticipantRepository.findByVoiceRoomId(existing.getId())).willReturn(List.of());

		VoiceRoomResponse response = voiceRoomService.createVoiceRoom(callerId, null, chatRoomId);

		assertThat(response.id()).isEqualTo(existing.getId());
		verify(voiceRoomRepository, never()).save(any(VoiceRoom.class));
		verify(notificationPublicService, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void getVoiceRoomRequiresRoomMemberAndFetchesParticipantNamesInBatch() {
		UUID requesterId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(requesterId, roomId);
		VoiceParticipant requester = participant(voiceRoom.getId(), requesterId);
		VoiceParticipant other = participant(voiceRoom.getId(), otherUserId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(requester, other));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any())).willReturn(Map.of(
				requesterId, user(requesterId, "미연"),
				otherUserId, user(otherUserId, "수진")
		));

		VoiceRoomResponse response = voiceRoomService.getVoiceRoom(requesterId, voiceRoom.getId());

		verify(projectRoomAccessPublicService).requireRoomMember(roomId, requesterId);
		verify(userPublicService, times(1)).getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any());
		verify(userPublicService, never()).getUser(any());
		assertThat(response.participants()).extracting("userName").containsExactly("미연", "수진");
		assertThat(response.participants()).extracting("micStatus").containsExactly("UNMUTED", "UNMUTED");
	}

	@Test
	void getOpenVoiceRoomByProjectRoomRequiresRoomMemberAndReturnsMicStatus() {
		UUID requesterId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(requesterId, roomId);
		VoiceParticipant requester = participant(voiceRoom.getId(), requesterId);
		VoiceParticipant other = participant(voiceRoom.getId(), otherUserId);
		other.updateMicStatus("MUTED");
		given(voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN)).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(requester, other));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any())).willReturn(Map.of(
				requesterId, user(requesterId, "미연"),
				otherUserId, user(otherUserId, "수진")
		));

		VoiceRoomResponse response = voiceRoomService.getOpenVoiceRoomByProjectRoom(requesterId, roomId);

		verify(projectRoomAccessPublicService).requireRoomMember(roomId, requesterId);
		assertThat(response.id()).isEqualTo(voiceRoom.getId());
		assertThat(response.participants()).extracting("userName").containsExactly("미연", "수진");
		assertThat(response.participants()).extracting("micStatus").containsExactly("UNMUTED", "MUTED");
	}

	@Test
	void getOpenVoiceRoomByChatRoomRequiresActiveMemberAndReturnsRoom() {
		UUID creatorId = UUID.randomUUID();
		UUID requesterId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		VoiceRoom voiceRoom = withId(VoiceRoom.createForChatRoom(chatRoomId, creatorId));
		VoiceParticipant creator = participant(voiceRoom.getId(), creatorId);
		given(voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN)).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(creator));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(creatorId, user(creatorId)));

		VoiceRoomResponse response = voiceRoomService.getOpenVoiceRoomByChatRoom(requesterId, chatRoomId);

		verify(chatRoomAccessPublicService).assertActiveMember(requesterId, chatRoomId);
		assertThat(response.id()).isEqualTo(voiceRoom.getId());
		assertThat(response.chatRoomId()).isEqualTo(chatRoomId);
	}

	@Test
	void getOpenVoiceRoomByChatRoomThrowsWhenNoOpenRoomExists() {
		UUID requesterId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		given(voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN)).willReturn(Optional.empty());

		assertThatThrownBy(() -> voiceRoomService.getOpenVoiceRoomByChatRoom(requesterId, chatRoomId))
				.isInstanceOf(com.bubli.global.error.BusinessException.class);
	}

	@Test
	void updateMicStatusRecordsParticipantMicEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));
		given(userPublicService.getUser(userId)).willReturn(user(userId));

		VoiceParticipantResponse response = voiceRoomService.updateMicStatus(userId, voiceRoom.getId(), "MUTED");

		assertThat(response.micStatus()).isEqualTo("MUTED");
		verify(projectRoomEventPublicService).recordVoiceParticipantMicUpdated(
				userId,
				roomId,
				voiceRoom.getId(),
				participant.getId(),
				userId,
				"MUTED"
		);
	}

	@Test
	void updateMicStatusOnlyAffectsLatestJoinedParticipant() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant oldLeft = participant(voiceRoom.getId(), userId);
		oldLeft.leave();
		VoiceParticipant latestLeft = participant(voiceRoom.getId(), userId);
		latestLeft.leave();

		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), userId))
				.willReturn(Optional.of(latestLeft));

		assertThatThrownBy(() -> voiceRoomService.updateMicStatus(userId, voiceRoom.getId(), "MUTED"))
				.isInstanceOf(com.bubli.global.error.BusinessException.class);
	}

	@Test
	void issueTokenRejoinsLeftParticipantAndReturnsToken() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		participant.leave();

		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));

		assertThat(voiceRoomService.issueToken(userId, voiceRoom.getId()).voiceRoomId()).isEqualTo(voiceRoom.getId());

		assertThat(participant.getStatus()).isEqualTo(VoiceParticipantStatus.JOINED);
		assertThat(participant.getLeftAt()).isNull();
		verify(projectRoomEventPublicService).recordVoiceParticipantJoined(
				userId,
				roomId,
				voiceRoom.getId(),
				participant.getId(),
				userId
		);
	}

	@Test
	void issueTokenCreatesParticipantWhenNotExist() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID anotherUserId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant saved = participant(voiceRoom.getId(), anotherUserId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), anotherUserId))
				.willReturn(Optional.empty());
		given(voiceParticipantRepository.save(any(VoiceParticipant.class))).willReturn(saved);

		voiceRoomService.issueToken(anotherUserId, voiceRoom.getId());

		verify(voiceParticipantRepository).save(any(VoiceParticipant.class));
		verify(projectRoomEventPublicService).recordVoiceParticipantJoined(
				anotherUserId,
				roomId,
				voiceRoom.getId(),
				saved.getId(),
				anotherUserId
		);
	}

	@Test
	void leaveVoiceRoomRecordsParticipantLeftEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(userId, user(userId)));

		voiceRoomService.leaveVoiceRoom(userId, voiceRoom.getId());

		assertThat(participant.getStatus()).isEqualTo(VoiceParticipantStatus.LEFT);
		verify(projectRoomEventPublicService).recordVoiceParticipantLeft(
				userId,
				roomId,
				voiceRoom.getId(),
				participant.getId(),
				userId
		);
	}

	@Test
	void leaveVoiceRoomNoopWhenLatestStatusIsLeft() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		participant.leave();

		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));

		voiceRoomService.leaveVoiceRoom(userId, voiceRoom.getId());

		verify(projectRoomEventPublicService, never()).recordVoiceParticipantLeft(any(), any(), any(), any(), any());
	}

	@Test
	void endVoiceRoomRecordsRoomEndedEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(userId, user(userId)));

		voiceRoomService.endVoiceRoom(userId, voiceRoom.getId());

		assertThat(voiceRoom.getStatus()).isEqualTo(VoiceRoomStatus.ENDED);
		verify(projectRoomEventPublicService).recordVoiceRoomEnded(userId, roomId, voiceRoom.getId());
	}

	@Test
	void endVoiceRoomRejectsNonCreatorForProjectRoomVoice() {
		UUID creatorId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(creatorId, roomId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));

		assertThatThrownBy(() -> voiceRoomService.endVoiceRoom(otherUserId, voiceRoom.getId()))
				.isInstanceOf(com.bubli.global.error.BusinessException.class);
		assertThat(voiceRoom.getStatus()).isEqualTo(VoiceRoomStatus.OPEN);
	}

	@Test
	void endVoiceRoomAllowsNonCreatorForDirectChatVoice() {
		UUID callerId = UUID.randomUUID();
		UUID calleeId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		VoiceRoom voiceRoom = withId(VoiceRoom.createForChatRoom(chatRoomId, callerId));
		VoiceParticipant participant = participant(voiceRoom.getId(), callerId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(chatRoomAccessPublicService.isDirectChatRoom(chatRoomId)).willReturn(true);
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(callerId, user(callerId)));

		voiceRoomService.endVoiceRoom(calleeId, voiceRoom.getId());

		verify(chatRoomAccessPublicService).assertActiveMember(calleeId, chatRoomId);
		assertThat(voiceRoom.getStatus()).isEqualTo(VoiceRoomStatus.ENDED);
	}

	@Test
	void declineVoiceRoomNotifiesCreator() {
		UUID callerId = UUID.randomUUID();
		UUID declinerId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		VoiceRoom voiceRoom = withId(VoiceRoom.createForChatRoom(chatRoomId, callerId));
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(userPublicService.getUser(declinerId)).willReturn(user(declinerId, "재민"));

		voiceRoomService.declineVoiceRoom(declinerId, voiceRoom.getId());

		verify(chatRoomAccessPublicService).assertActiveMember(declinerId, chatRoomId);
		verify(notificationPublicService).create(
				callerId,
				com.bubli.personal.notification.type.NotificationSourceType.VOICE_CALL_DECLINED,
				chatRoomId,
				"재민",
				"통화를 거절했습니다"
		);
	}

	@Test
	void declineVoiceRoomRejectsProjectRoomVoiceRoom() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));

		assertThatThrownBy(() -> voiceRoomService.declineVoiceRoom(userId, voiceRoom.getId()))
				.isInstanceOf(com.bubli.global.error.BusinessException.class);
		verify(notificationPublicService, never()).create(any(), any(), any(), any(), any());
	}

	@Test
	void declineVoiceRoomIsNoopWhenAlreadyEnded() {
		UUID callerId = UUID.randomUUID();
		UUID declinerId = UUID.randomUUID();
		UUID chatRoomId = UUID.randomUUID();
		VoiceRoom voiceRoom = withId(VoiceRoom.createForChatRoom(chatRoomId, callerId));
		voiceRoom.end();
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));

		voiceRoomService.declineVoiceRoom(declinerId, voiceRoom.getId());

		verify(chatRoomAccessPublicService, never()).assertActiveMember(any(), any());
		verify(notificationPublicService, never()).create(any(), any(), any(), any(), any());
	}

	private VoiceRoom voiceRoom(UUID userId, UUID roomId) {
		return withId(VoiceRoom.create(roomId, userId));
	}

	private VoiceParticipant participant(UUID voiceRoomId, UUID userId) {
		return withId(VoiceParticipant.join(voiceRoomId, userId));
	}

	private VoiceRoom withId(VoiceRoom voiceRoom) {
		ReflectionTestUtils.setField(voiceRoom, "id", UUID.randomUUID());
		ReflectionTestUtils.setField(voiceRoom, "createdAt", Instant.now());
		return voiceRoom;
	}

	private VoiceParticipant withId(VoiceParticipant participant) {
		ReflectionTestUtils.setField(participant, "id", UUID.randomUUID());
		return participant;
	}

	private UserResult user(UUID userId) {
		return user(userId, "미연");
	}

	private UserResult user(UUID userId, String name) {
		return new UserResult(userId, "bubli-id", name, null, "ko-KR", "Asia/Seoul");
	}
}
