package com.bubli.project.service;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.ProjectRoomEvent;
import com.bubli.project.repository.ProjectRoomEventRepository;
import com.bubli.project.type.PaymentStatus;
import com.bubli.project.type.ProjectRoomStatus;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomEventRecorderTest {

	private final ProjectRoomEventRepository projectRoomEventRepository = mock(ProjectRoomEventRepository.class);
	private final WebSocketPublishPublicService webSocketPublishPublicService = mock(WebSocketPublishPublicService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ProjectRoomEventRecorder recorder = new ProjectRoomEventRecorder(
			projectRoomEventRepository,
			objectMapper,
			webSocketPublishPublicService
	);

	@Test
	void recordRoomUpdatedStoresNextSequenceAndJsonPayload() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ProjectRoom projectRoom = ProjectRoom.create(
				actorUserId,
				"새 프로젝트",
				"새 클라이언트",
				null,
				PaymentStatus.NOT_RECORDED,
				null,
				null,
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);
		ProjectRoomEvent previousEvent = ProjectRoomEvent.create(
				roomId,
				2L,
				"ROOM_CREATED",
				actorUserId,
				"{\"roomId\":\"" + roomId + "\"}",
				null
		);
		when(projectRoomEventRepository.findTopByRoomIdOrderBySequenceDesc(roomId))
				.thenReturn(Optional.of(previousEvent));
		when(projectRoomEventRepository.save(any(ProjectRoomEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		recorder.recordRoomUpdated(actorUserId, projectRoom);

		ArgumentCaptor<ProjectRoomEvent> eventCaptor = ArgumentCaptor.forClass(ProjectRoomEvent.class);
		verify(projectRoomEventRepository).save(eventCaptor.capture());
		ProjectRoomEvent savedEvent = eventCaptor.getValue();
		assertThat(savedEvent.getRoomId()).isEqualTo(roomId);
		assertThat(savedEvent.getSequence()).isEqualTo(3L);
		assertThat(savedEvent.getEventType()).isEqualTo(ProjectRoomEventRecorder.ROOM_UPDATED);
		assertThat(savedEvent.getActorUserId()).isEqualTo(actorUserId);
		var payload = objectMapper.readTree(savedEvent.getPayloadJson());
		assertThat(payload.get("roomId").asText()).isEqualTo(roomId.toString());
		assertThat(payload.get("name").asText()).isEqualTo("새 프로젝트");
		assertThat(payload.get("clientName").asText()).isEqualTo("새 클라이언트");
		assertThat(payload.get("status").asText()).isEqualTo("ACTIVE");
		verify(webSocketPublishPublicService).publishProjectRoomEvent(
				savedEvent.getId(),
				savedEvent.getEventType(),
				savedEvent.getRoomId(),
				savedEvent.getSequence(),
				savedEvent.getOccurredAt(),
				savedEvent.getActorUserId(),
				savedEvent.getPayloadJson()
		);
	}

	@Test
	void recordPaymentUpdatedStartsSequenceAtOneWhenRoomHasNoEvents() throws Exception {
		UUID actorUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ProjectRoom projectRoom = ProjectRoom.create(
				actorUserId,
				"입금 관리 프로젝트",
				null,
				BigDecimal.valueOf(2_000_000),
				PaymentStatus.PAID,
				LocalDate.parse("2026-07-20"),
				LocalDate.parse("2026-07-18"),
				ProjectRoomStatus.ACTIVE
		);
		ReflectionTestUtils.setField(projectRoom, "id", roomId);
		when(projectRoomEventRepository.findTopByRoomIdOrderBySequenceDesc(roomId))
				.thenReturn(Optional.empty());
		when(projectRoomEventRepository.save(any(ProjectRoomEvent.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		recorder.recordPaymentUpdated(actorUserId, projectRoom);

		ArgumentCaptor<ProjectRoomEvent> eventCaptor = ArgumentCaptor.forClass(ProjectRoomEvent.class);
		verify(projectRoomEventRepository).save(eventCaptor.capture());
		ProjectRoomEvent savedEvent = eventCaptor.getValue();
		assertThat(savedEvent.getSequence()).isEqualTo(1L);
		assertThat(savedEvent.getEventType()).isEqualTo(ProjectRoomEventRecorder.ROOM_PAYMENT_UPDATED);
		var payload = objectMapper.readTree(savedEvent.getPayloadJson());
		assertThat(payload.get("paymentStatus").asText()).isEqualTo("PAID");
		assertThat(payload.get("contractAmount").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(2_000_000));
		assertThat(payload.get("paymentDueDate").asText()).isEqualTo("2026-07-20");
		assertThat(payload.get("paidAt").asText()).isEqualTo("2026-07-18");
		verify(webSocketPublishPublicService).publishProjectRoomEvent(
				savedEvent.getId(),
				savedEvent.getEventType(),
				savedEvent.getRoomId(),
				savedEvent.getSequence(),
				savedEvent.getOccurredAt(),
				savedEvent.getActorUserId(),
				savedEvent.getPayloadJson()
		);
	}
}
