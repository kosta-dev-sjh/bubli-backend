package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.SequenceListResponse;
import com.bubli.project.dto.ProjectRoomEventActorResponse;
import com.bubli.project.dto.ProjectRoomEventResponse;
import com.bubli.project.entity.ProjectRoomEvent;
import com.bubli.project.repository.ProjectRoomEventRepository;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomEventService {

	private static final int DEFAULT_LIMIT = 100;
	private static final int MAX_LIMIT = 100;

	private final ProjectRoomService projectRoomService;
	private final ProjectRoomEventRepository projectRoomEventRepository;
	private final UserPublicService userPublicService;
	private final ObjectMapper objectMapper;

	@Transactional(readOnly = true)
	public SequenceListResponse<ProjectRoomEventResponse> getEvents(
			UUID userId,
			UUID roomId,
			Long afterSequence,
			Integer limit
	) {
		projectRoomService.getProjectRoom(userId, roomId);

		long normalizedAfterSequence = afterSequence == null ? 0L : Math.max(afterSequence, 0L);
		int normalizedLimit = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));
		Page<ProjectRoomEvent> page = projectRoomEventRepository.findByRoomIdAndSequenceGreaterThanOrderBySequenceAsc(
				roomId,
				normalizedAfterSequence,
				PageRequest.of(0, normalizedLimit)
		);
		long latestSequence = projectRoomEventRepository.findTopByRoomIdOrderBySequenceDesc(roomId)
				.map(ProjectRoomEvent::getSequence)
				.orElse(0L);
		Long lastReceivedSequence = page.getContent().stream()
				.map(ProjectRoomEvent::getSequence)
				.reduce((ignored, sequence) -> sequence)
				.orElse(null);

		List<ProjectRoomEvent> events = page.getContent();
		Map<UUID, UserResult> actors = userPublicService.getUsers(events.stream()
				.map(ProjectRoomEvent::getActorUserId)
				.filter(Objects::nonNull)
				.distinct()
				.toList());

		return new SequenceListResponse<>(
				events.stream()
						.map(event -> toResponse(event, actors))
						.toList(),
				lastReceivedSequence,
				latestSequence,
				page.hasNext()
		);
	}

	private ProjectRoomEventResponse toResponse(ProjectRoomEvent event, Map<UUID, UserResult> actors) {
		return new ProjectRoomEventResponse(
				event.getId(),
				event.getEventType(),
				event.getRoomId(),
				event.getSequence(),
				event.getOccurredAt(),
				actor(event, actors),
				payload(event)
		);
	}

	private ProjectRoomEventActorResponse actor(ProjectRoomEvent event, Map<UUID, UserResult> actors) {
		if (event.getActorUserId() == null) {
			return ProjectRoomEventActorResponse.system();
		}
		UserResult user = actors.get(event.getActorUserId());
		if (user == null) {
			return ProjectRoomEventActorResponse.user(event.getActorUserId(), "Unknown");
		}
		return ProjectRoomEventActorResponse.user(user.id(), user.name());
	}

	private JsonNode payload(ProjectRoomEvent event) {
		try {
			return objectMapper.readTree(event.getPayloadJson());
		} catch (JsonProcessingException e) {
			throw new BusinessException(ErrorCode.COMMON_500_001);
		}
	}
}
