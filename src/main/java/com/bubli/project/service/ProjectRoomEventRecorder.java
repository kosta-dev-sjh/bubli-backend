package com.bubli.project.service;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.ProjectRoomEvent;
import com.bubli.project.repository.ProjectRoomEventRepository;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomEventRecorder {

	static final String ROOM_UPDATED = "ROOM_UPDATED";
	static final String ROOM_PAYMENT_UPDATED = "ROOM_PAYMENT_UPDATED";
	static final String ROOM_CLOSED = "ROOM_CLOSED";

	private final ProjectRoomEventRepository projectRoomEventRepository;
	private final ObjectMapper objectMapper;
	private final WebSocketPublishPublicService webSocketPublishPublicService;

	public void recordRoomUpdated(UUID actorUserId, ProjectRoom projectRoom) {
		ObjectNode payload = objectMapper.createObjectNode()
				.put("roomId", projectRoom.getId().toString())
				.put("name", projectRoom.getName())
				.put("status", projectRoom.getStatus().name());
		putNullable(payload, "clientName", projectRoom.getClientName());
		putNullable(payload, "closedAt", projectRoom.getClosedAt());
		save(actorUserId, projectRoom, ROOM_UPDATED, payload);
	}

	public void recordPaymentUpdated(UUID actorUserId, ProjectRoom projectRoom) {
		ObjectNode payload = objectMapper.createObjectNode()
				.put("roomId", projectRoom.getId().toString())
				.put("paymentStatus", projectRoom.getPaymentStatus().name());
		putNullable(payload, "contractAmount", projectRoom.getContractAmount());
		putNullable(payload, "paymentDueDate", projectRoom.getPaymentDueDate());
		putNullable(payload, "paidAt", projectRoom.getPaidAt());
		save(actorUserId, projectRoom, ROOM_PAYMENT_UPDATED, payload);
	}

	public void recordRoomClosed(UUID actorUserId, ProjectRoom projectRoom) {
		ObjectNode payload = objectMapper.createObjectNode()
				.put("roomId", projectRoom.getId().toString())
				.put("status", projectRoom.getStatus().name());
		putNullable(payload, "closedAt", projectRoom.getClosedAt());
		save(actorUserId, projectRoom, ROOM_CLOSED, payload);
	}

	private void save(UUID actorUserId, ProjectRoom projectRoom, String eventType, ObjectNode payload) {
		Long sequence = nextSequence(projectRoom.getId());
		ProjectRoomEvent event = projectRoomEventRepository.save(ProjectRoomEvent.create(
				projectRoom.getId(),
				sequence,
				eventType,
				actorUserId,
				payload.toString(),
				Instant.now()
		));
		webSocketPublishPublicService.publishProjectRoomEvent(
				event.getId(),
				event.getEventType(),
				event.getRoomId(),
				event.getSequence(),
				event.getOccurredAt(),
				event.getActorUserId(),
				event.getPayloadJson()
		);
	}

	private Long nextSequence(UUID roomId) {
		return projectRoomEventRepository.findTopByRoomIdOrderBySequenceDesc(roomId)
				.map(ProjectRoomEvent::getSequence)
				.map(sequence -> sequence + 1)
				.orElse(1L);
	}

	private void putNullable(ObjectNode payload, String fieldName, String value) {
		if (value == null) {
			payload.putNull(fieldName);
			return;
		}
		payload.put(fieldName, value);
	}

	private void putNullable(ObjectNode payload, String fieldName, Instant value) {
		if (value == null) {
			payload.putNull(fieldName);
			return;
		}
		payload.put(fieldName, value.toString());
	}

	private void putNullable(ObjectNode payload, String fieldName, LocalDate value) {
		if (value == null) {
			payload.putNull(fieldName);
			return;
		}
		payload.put(fieldName, value.toString());
	}

	private void putNullable(ObjectNode payload, String fieldName, BigDecimal value) {
		if (value == null) {
			payload.putNull(fieldName);
			return;
		}
		payload.put(fieldName, value);
	}
}
