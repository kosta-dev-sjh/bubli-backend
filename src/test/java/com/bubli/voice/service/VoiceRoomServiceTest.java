package com.bubli.voice.service;

import com.bubli.project.service.ProjectRoomAccessPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.voice.config.LiveKitProperties;
import com.bubli.voice.dto.VoiceRoomResponse;
import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.repository.VoiceParticipantRepository;
import com.bubli.voice.repository.VoiceRoomRepository;
import com.bubli.voice.type.VoiceParticipantStatus;
import com.bubli.voice.type.VoiceRoomStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
	UserPublicService userPublicService;

	VoiceRoomService voiceRoomService;

	@BeforeEach
	void setUp() {
		voiceRoomService = new VoiceRoomService(
				voiceRoomRepository,
				voiceParticipantRepository,
				projectRoomAccessPublicService,
				projectRoomEventPublicService,
				userPublicService,
				new LiveKitProperties("api-key", "test-secret-key-must-be-at-least-32-bytes", "wss://livekit.example")
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

		VoiceRoomResponse response = voiceRoomService.createVoiceRoom(userId, roomId);

		assertThat(response.roomId()).isEqualTo(roomId);
		verify(projectRoomAccessPublicService).requireRoomMember(roomId, userId);
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
	void updateMicStatusRecordsParticipantMicEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndUserId(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));
		given(userPublicService.getUser(userId)).willReturn(user(userId));

		voiceRoomService.updateMicStatus(userId, voiceRoom.getId(), "MUTED");

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
	void leaveVoiceRoomRecordsParticipantLeftEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndUserId(voiceRoom.getId(), userId))
				.willReturn(Optional.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));
		given(userPublicService.getUser(userId)).willReturn(user(userId));

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
	void endVoiceRoomRecordsRoomEndedEvent() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		VoiceRoom voiceRoom = voiceRoom(userId, roomId);
		VoiceParticipant participant = participant(voiceRoom.getId(), userId);
		given(voiceRoomRepository.findById(voiceRoom.getId())).willReturn(Optional.of(voiceRoom));
		given(voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoom.getId(), VoiceParticipantStatus.JOINED))
				.willReturn(List.of(participant));
		given(voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId())).willReturn(List.of(participant));
		given(userPublicService.getUser(userId)).willReturn(user(userId));

		voiceRoomService.endVoiceRoom(userId, voiceRoom.getId());

		assertThat(voiceRoom.getStatus()).isEqualTo(VoiceRoomStatus.ENDED);
		verify(projectRoomEventPublicService).recordVoiceRoomEnded(userId, roomId, voiceRoom.getId());
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
		return new UserResult(userId, "bubli-id", "미연", null, "ko-KR", "Asia/Seoul");
	}
}
