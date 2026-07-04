package com.bubli.project.service;

import com.bubli.global.response.SequenceListResponse;
import com.bubli.project.dto.ProjectRoomEventResponse;
import com.bubli.project.entity.ProjectRoomEvent;
import com.bubli.project.repository.ProjectRoomEventRepository;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectRoomEventServiceTest {

	@Mock
	ProjectRoomService projectRoomService;

	@Mock
	ProjectRoomEventRepository projectRoomEventRepository;

	@Mock
	UserPublicService userPublicService;

	@Spy
	ObjectMapper objectMapper = new ObjectMapper();

	@InjectMocks
	ProjectRoomEventService projectRoomEventService;

	@Test
	void getEventsBatchLoadsActorsAndKeepsUnknownFallback() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID missingActorId = UUID.randomUUID();
		ProjectRoomEvent first = event(roomId, 1L, "ROOM_UPDATED", actorId);
		ProjectRoomEvent second = event(roomId, 2L, "SYSTEM_EVENT", null);
		ProjectRoomEvent third = event(roomId, 3L, "ROOM_PAYMENT_UPDATED", missingActorId);
		PageRequest pageable = PageRequest.of(0, 50);
		given(projectRoomEventRepository.findByRoomIdAndSequenceGreaterThanOrderBySequenceAsc(
				roomId,
				0L,
				pageable
		)).willReturn(new PageImpl<>(List.of(first, second, third), pageable, 3));
		given(projectRoomEventRepository.findTopByRoomIdOrderBySequenceDesc(roomId))
				.willReturn(Optional.of(third));
		given(userPublicService.getUsers(org.mockito.ArgumentMatchers.<Collection<UUID>>any()))
				.willReturn(Map.of(actorId, user(actorId, "민서")));

		SequenceListResponse<ProjectRoomEventResponse> result = projectRoomEventService.getEvents(
				userId,
				roomId,
				null,
				50
		);

		assertThat(result.getItems()).hasSize(3);
		assertThat(result.getLastReceivedSequence()).isEqualTo(3L);
		assertThat(result.getLatestSequence()).isEqualTo(3L);
		assertThat(result.getItems().get(0).actor().id()).isEqualTo(actorId);
		assertThat(result.getItems().get(0).actor().name()).isEqualTo("민서");
		assertThat(result.getItems().get(1).actor().type()).isEqualTo("SYSTEM");
		assertThat(result.getItems().get(2).actor().id()).isEqualTo(missingActorId);
		assertThat(result.getItems().get(2).actor().name()).isEqualTo("Unknown");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Collection<UUID>> actorIdsCaptor = ArgumentCaptor.forClass(Collection.class);
		verify(userPublicService).getUsers(actorIdsCaptor.capture());
		assertThat(actorIdsCaptor.getValue()).containsExactlyInAnyOrder(actorId, missingActorId);
		verify(userPublicService, never()).getUser(any());
	}

	private ProjectRoomEvent event(UUID roomId, Long sequence, String eventType, UUID actorUserId) {
		ProjectRoomEvent event = ProjectRoomEvent.create(
				roomId,
				sequence,
				eventType,
				actorUserId,
				"{}",
				Instant.now()
		);
		ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
		return event;
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
}
