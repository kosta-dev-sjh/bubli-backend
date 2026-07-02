package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobContext;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.dto.ChatMessageResult;
import com.bubli.chat.service.ChatMessagePublicService;
import com.bubli.chat.type.MessageType;
import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.service.RoomMemoryPublicService;
import com.bubli.memory.type.SummaryStatus;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.user.service.UserLocalePublicService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomAgentCommandServiceTest {

	@Test
	void suggestCommandCreatesDraftSuggestionAndIncludesSuggestionIdInResponseBody() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ObjectMapper objectMapper = new ObjectMapper();

		when(suggestionCommandService.createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(resourceId),
				eq(AgentSuggestionType.REVIEW_ITEM),
				any(),
				any()
		)).thenReturn(suggestionResponse(suggestionId, userId, roomId, resourceId, AgentSuggestionType.REVIEW_ITEM));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				objectMapper
		).execute(
				userId,
				roomId,
				"review contract risk",
				AgentCommandMode.SUGGEST,
				List.of(resourceId)
		);

		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.suggestions().getFirst().suggestionId()).isEqualTo(suggestionId);
		assertThat(response.message().body().get("suggestionIds").get(0).asText()).isEqualTo(suggestionId.toString());
		verify(eventPublicService).recordAgentSuggestionsCreated(
				userId,
				roomId,
				List.of(suggestionId),
				List.of(AgentSuggestionType.REVIEW_ITEM.name())
		);
	}

	@Test
	void answerCommandDoesNotCreateSuggestion() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper()
		).execute(
				userId,
				roomId,
				"answer this",
				AgentCommandMode.ANSWER,
				List.of()
		);

		assertThat(response.suggestions()).isEmpty();
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
		verify(eventPublicService, never()).recordAgentSuggestionsCreated(any(), any(), any(), any());
	}

	@Test
	void answerCommandAllowsMissingResourceIds() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper()
		).execute(
				userId,
				roomId,
				"answer this",
				AgentCommandMode.ANSWER,
				null
		);

		assertThat(response.message().resourceId()).isNull();
		assertThat(response.suggestions()).isEmpty();
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void fallbackAnswerUsesResolvedUserLocale() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper(),
				"en-US"
		).execute(
				userId,
				roomId,
				"answer this",
				AgentCommandMode.ANSWER,
				List.of()
		);

		assertThat(response.message().body().get("text").asText())
				.contains("I checked the currently collected project context.");
	}

	@Test
	void latestResourceCommandIncludesResolvedResourceInAgentResponse() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);

		when(resourcePublicService.findLatestRoomResource(userId, roomId, List.of("계약", "contract", "agreement")))
				.thenReturn(Optional.of(resource(resourceId, userId, roomId, "NDA_최종본.pdf")));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper(),
				"ko-KR",
				resourcePublicService
		).execute(
				userId,
				roomId,
				"가장 최근에 올라온 계약서 알려줘",
				AgentCommandMode.ANSWER,
				List.of()
		);

		assertThat(response.message().resourceId()).isEqualTo(resourceId);
		assertThat(response.message().body().get("text").asText()).contains("NDA_최종본.pdf");
		assertThat(response.message().body().get("resources").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@Test
	void latestUploadedFileCommandUsesLatestRoomFileLookup() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);

		when(resourcePublicService.findLatestRoomFile(userId, roomId))
				.thenReturn(Optional.of(resource(resourceId, userId, roomId, "latest-upload.pdf")));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper(),
				"ko-KR",
				resourcePublicService
		).execute(
				userId,
				roomId,
				"latest uploaded file",
				AgentCommandMode.ANSWER,
				List.of()
		);

		assertThat(response.message().resourceId()).isEqualTo(resourceId);
		assertThat(response.message().body().get("text").asText()).contains("latest-upload.pdf");
		verify(resourcePublicService).findLatestRoomFile(userId, roomId);
	}

	@Test
	void suggestCommandInfersWbsSuggestionType() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);

		when(suggestionCommandService.createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(null),
				eq(AgentSuggestionType.WBS),
				any(),
				any()
		)).thenReturn(suggestionResponse(suggestionId, userId, roomId, null, AgentSuggestionType.WBS));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				new ObjectMapper()
		).execute(
				userId,
				roomId,
				"다음에 해야 할 WBS 제안해줘",
				AgentCommandMode.SUGGEST,
				List.of()
		);

		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.suggestions().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.WBS);
	}

	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			ObjectMapper objectMapper
	) {
		return service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				objectMapper,
				"ko-KR",
				mock(ResourcePublicService.class)
		);
	}

	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			ObjectMapper objectMapper,
			String locale
	) {
		return service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				objectMapper,
				locale,
				mock(ResourcePublicService.class)
		);
	}

	@SuppressWarnings("unchecked")
	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			ObjectMapper objectMapper,
			String locale,
			ResourcePublicService resourcePublicService
	) {
		AgentJobContextCollector contextCollector = mock(AgentJobContextCollector.class);
		when(contextCollector.collect(any())).thenReturn(new AgentJobContext("context", 7));
		ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
		when(chatModelProvider.getIfAvailable()).thenReturn(null);
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn(locale);
		return new ProjectRoomAgentCommandService(
				mock(ProjectMembershipPublicService.class),
				contextCollector,
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				resourcePublicService,
				userLocalePublicService,
				chatModelProvider,
				mock(ObjectProvider.class),
				objectMapper
		);
	}

	private AgentSuggestionResponse suggestionResponse(
			UUID suggestionId,
			UUID userId,
			UUID roomId,
			UUID resourceId,
			AgentSuggestionType suggestionType
	) {
		return new AgentSuggestionResponse(
				suggestionId,
				userId,
				roomId,
				null,
				resourceId,
				suggestionType,
				AgentSuggestionStatus.DRAFT,
				Map.of("title", "review contract risk"),
				Map.of("source", "PROJECT_ROOM_AGENT_COMMAND"),
				null,
				null,
				Instant.now(),
				Instant.now()
		);
	}

	private ChatMessageResult chatMessage(JsonNode body, UUID resourceId) {
		return new ChatMessageResult(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"AGENT",
				null,
				"Bubli Agent",
				null,
				10L,
				MessageType.AGENT_RESPONSE,
				body,
				resourceId,
				Instant.now()
		);
	}

	private RoomMemorySummaryContextResult memory() {
		return new RoomMemorySummaryContextResult(
				UUID.randomUUID(),
				10L,
				10L,
				"{}",
				SummaryStatus.DRAFT,
				Instant.now()
		);
	}

	private ResourceResult resource(UUID resourceId, UUID userId, UUID roomId, String title) {
		return new ResourceResult(
				resourceId,
				userId,
				roomId,
				title,
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				ResourceStatus.READY,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}
}
