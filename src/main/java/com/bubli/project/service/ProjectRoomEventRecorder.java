package com.bubli.project.service;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.ProjectRoomEvent;
import com.bubli.project.repository.ProjectRoomEventRepository;
import com.bubli.websocket.service.WebSocketPublishPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomEventRecorder {

	static final String ROOM_UPDATED = "ROOM_UPDATED";
	static final String ROOM_PAYMENT_UPDATED = "ROOM_PAYMENT_UPDATED";
	static final String ROOM_CLOSED = "ROOM_CLOSED";
	static final String AGENT_SUGGESTIONS_CREATED = "AGENT_SUGGESTIONS_CREATED";
	static final String AGENT_SUGGESTION_APPROVED = "AGENT_SUGGESTION_APPROVED";
	static final String AGENT_SUGGESTION_REJECTED = "AGENT_SUGGESTION_REJECTED";
	static final String AGENT_SUGGESTION_HELD = "AGENT_SUGGESTION_HELD";
	static final String AGENT_SUGGESTION_UPDATED = "AGENT_SUGGESTION_UPDATED";
	static final String AGENT_SUGGESTION_DELETED = "AGENT_SUGGESTION_DELETED";

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

	public void recordAgentSuggestionsCreated(
			UUID actorUserId,
			UUID roomId,
			List<UUID> suggestionIds,
			List<String> suggestionTypes
	) {
		ObjectNode payload = objectMapper.createObjectNode()
				.put("roomId", roomId.toString())
				.put("count", suggestionIds.size());
		ArrayNode ids = payload.putArray("suggestionIds");
		suggestionIds.forEach(id -> ids.add(id.toString()));
		ArrayNode types = payload.putArray("suggestionTypes");
		suggestionTypes.forEach(types::add);
		save(actorUserId, roomId, AGENT_SUGGESTIONS_CREATED, payload);
	}

	public void recordAgentSuggestionReviewed(
			UUID actorUserId,
			UUID roomId,
			UUID suggestionId,
			String suggestionType,
			String status,
			String action
	) {
		ObjectNode payload = objectMapper.createObjectNode()
				.put("roomId", roomId.toString())
				.put("suggestionId", suggestionId.toString())
				.put("suggestionType", suggestionType)
				.put("status", status)
				.put("action", action);
		save(actorUserId, roomId, agentSuggestionReviewEventType(status, action), payload);
	}

	private String agentSuggestionReviewEventType(String status, String action) {
		if ("DELETE".equals(action)) {
			return AGENT_SUGGESTION_DELETED;
		}
		return switch (status) {
			case "APPROVED" -> AGENT_SUGGESTION_APPROVED;
			case "REJECTED" -> AGENT_SUGGESTION_REJECTED;
			case "HELD" -> AGENT_SUGGESTION_HELD;
			default -> AGENT_SUGGESTION_UPDATED;
		};
	}

	private void save(UUID actorUserId, ProjectRoom projectRoom, String eventType, ObjectNode payload) {
		save(actorUserId, projectRoom.getId(), eventType, payload);
	}

	private void save(UUID actorUserId, UUID roomId, String eventType, ObjectNode payload) {
		Long sequence = nextSequence(roomId);
		ProjectRoomEvent event = projectRoomEventRepository.save(ProjectRoomEvent.create(
				roomId,
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
