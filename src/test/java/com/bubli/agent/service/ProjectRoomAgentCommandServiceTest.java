package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomRagContext;
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
import com.bubli.resource.dto.ResourceSearchHit;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomAgentCommandServiceTest {

	@Test
	void returnsNoAnswerWithoutCallingLlmWhenRagContextIsEmpty() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
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
				"ko-KR",
				ProjectRoomRagContext.ungrounded(),
				chatModel
		).execute(userId, roomId, "/bubli 내 ID가 뭐야", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
		assertThat(response.message().body().get("text").asText()).isEqualTo("프로젝트 자료 기준에서는 알 수 없는 내용입니다.");
		assertThat(response.message().body().get("ragGrounded").asBoolean()).isFalse();
	}

	@Test
	void returnsLocalizedNoAnswer() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);

		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				mock(AgentSuggestionCommandService.class),
				mock(ProjectRoomEventPublicService.class),
				"ja-JP",
				ProjectRoomRagContext.ungrounded(),
				mock(ChatModel.class)
		).execute(userId, roomId, "/bubli 内容を教えて", AgentCommandMode.ANSWER, List.of());

		assertThat(response.message().body().get("text").asText()).isEqualTo("プロジェクト資料の範囲では分かりません。");
	}

	@Test
	void groundedAnswerPromptUsesOnlyRetrievedDocumentChunks() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ResourceSearchHit hit = hit(resourceId, "契約期間は2026年7月1日から2026年9月30日までです。", 0.93D);
		ProjectRoomRagContext ragContext = new ProjectRoomRagContext(true, List.of(hit), 0.93D, """
				[Source]
				resourceId=%s
				chunkIndex=0
				pageNumber=2
				similarityScore=0.93
				chunkText=
				契約期間は2026年7月1日から2026年9月30日までです。
				""".formatted(resourceId));

		when(chatModel.call(any(String.class))).thenReturn("契約期間は2026年7月1日から2026年9月30日までです。");
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				mock(AgentSuggestionCommandService.class),
				mock(ProjectRoomEventPublicService.class),
				"ja-JP",
				ragContext,
				chatModel
		).execute(userId, roomId, "/bubli 契約期間は?", AgentCommandMode.ANSWER, List.of());

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue()).contains("Use ONLY the project material sources");
		assertThat(promptCaptor.getValue()).contains("契約期間は2026年7月1日から2026年9月30日までです。");
		assertThat(promptCaptor.getValue()).doesNotContain("Recent room chat");
		assertThat(promptCaptor.getValue()).doesNotContain("Room memory summaries");
		assertThat(promptCaptor.getValue()).doesNotContain("Room tasks");
		assertThat(promptCaptor.getValue()).doesNotContain("Room WBS");
		assertThat(promptCaptor.getValue()).doesNotContain("Room schedules");
		assertThat(response.message().resourceId()).isEqualTo(resourceId);
		assertThat(response.message().body().get("ragGrounded").asBoolean()).isTrue();
		assertThat(response.message().body().get("ragHits").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@Test
	void suggestModeWithoutRagHitDoesNotCreateSuggestion() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);

		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				mock(ProjectRoomEventPublicService.class),
				"ko-KR",
				ProjectRoomRagContext.ungrounded(),
				chatModel
		).execute(userId, roomId, "/bubli todo 만들어줘", AgentCommandMode.SUGGEST, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
		assertThat(response.suggestions()).isEmpty();
	}

	@Test
	void suggestModeWithRagHitStoresRagEvidence() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ResourceSearchHit hit = hit(resourceId, "検収後7日以内に請求書を発行します。", 0.91D);
		ProjectRoomRagContext ragContext = new ProjectRoomRagContext(true, List.of(hit), 0.91D, "chunkText=\n検収後7日以内に請求書を発行します。");

		when(chatModel.call(any(String.class))).thenReturn("TODO: 検収後7日以内の請求書発行を確認する。");
		when(suggestionCommandService.createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(resourceId),
				eq(AgentSuggestionType.TODO),
				any(),
				any()
		)).thenReturn(suggestionResponse(suggestionId, userId, roomId, resourceId, AgentSuggestionType.TODO));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				"ja-JP",
				ragContext,
				chatModel
		).execute(userId, roomId, "/bubli todo 契約書の内容に基づいてTODO作って", AgentCommandMode.SUGGEST, List.of());

		verify(suggestionCommandService).createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(resourceId),
				eq(AgentSuggestionType.TODO),
				any(),
				org.mockito.ArgumentMatchers.assertArg(evidence -> {
					assertThat(evidence.get("ragGrounded")).isEqualTo(true);
					assertThat(evidence.get("ragMaxSimilarity")).isEqualTo(0.91D);
					assertThat(evidence.get("ragHits").toString()).contains(resourceId.toString());
				})
		);
		verify(eventPublicService).recordAgentSuggestionsCreated(
				userId,
				roomId,
				List.of(suggestionId),
				List.of(AgentSuggestionType.TODO.name())
		);
		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.message().body().get("ragHits")).hasSize(1);
	}

	@Test
	void resourceInventoryQuestionReturnsUploadedResourcesWithoutRagOrLlm() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ProjectRoomRagGroundingService ragGroundingService = mock(ProjectRoomRagGroundingService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);

		when(resourcePublicService.getRecentRoomResources(userId, roomId, 10))
				.thenReturn(List.of(resource(resourceId, userId, roomId, "契約書.pdf")));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				mock(AgentSuggestionCommandService.class),
				mock(ProjectRoomEventPublicService.class),
				"ko-KR",
				ProjectRoomRagContext.ungrounded(),
				chatModel,
				resourcePublicService,
				ragGroundingService
		).execute(userId, roomId, "/bubli 현재 프로젝트에 업로드된 파일이 뭐야?", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(ragGroundingService, never()).retrieve(any(), any(), any(), any(), any());
		assertThat(response.message().body().get("text").asText()).contains("契約書.pdf");
		assertThat(response.message().body().get("resources").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@Test
	void japaneseResourceInventoryQuestionReturnsUploadedResourcesWithoutRagOrLlm() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ProjectRoomRagGroundingService ragGroundingService = mock(ProjectRoomRagGroundingService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);

		when(resourcePublicService.getRecentRoomResources(userId, roomId, 10))
				.thenReturn(List.of(resource(resourceId, userId, roomId, "フロントエンド開発_契約書.pdf")));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(resourceId)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), resourceId));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				mock(AgentSuggestionCommandService.class),
				mock(ProjectRoomEventPublicService.class),
				"ja-JP",
				ProjectRoomRagContext.ungrounded(),
				chatModel,
				resourcePublicService,
				ragGroundingService
		).execute(userId, roomId, "/bubli どんなファイルがある？", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(ragGroundingService, never()).retrieve(any(), any(), any(), any(), any());
		assertThat(response.message().body().get("text").asText()).contains("フロントエンド開発_契約書.pdf");
		assertThat(response.message().body().get("resources").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@SuppressWarnings("unchecked")
	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			String locale,
			ProjectRoomRagContext ragContext,
			ChatModel chatModel
	) {
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn(locale);
		ProjectRoomRagGroundingService ragGroundingService = mock(ProjectRoomRagGroundingService.class);
		when(ragGroundingService.retrieve(any(UUID.class), any(UUID.class), any(String.class), eq(locale), any(AgentCommandMode.class)))
				.thenReturn(ragContext);
		ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
		when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
		ObjectProvider<com.bubli.agent.model.AiCallExecutor> aiCallExecutorProvider = mock(ObjectProvider.class);
		when(aiCallExecutorProvider.getIfAvailable()).thenReturn(null);
		return new ProjectRoomAgentCommandService(
				mock(ProjectMembershipPublicService.class),
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				userLocalePublicService,
				mock(ResourcePublicService.class),
				ragGroundingService,
				chatModelProvider,
				aiCallExecutorProvider,
				new ObjectMapper()
		);
	}

	@SuppressWarnings("unchecked")
	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			String locale,
			ProjectRoomRagContext ragContext,
			ChatModel chatModel,
			ResourcePublicService resourcePublicService,
			ProjectRoomRagGroundingService ragGroundingService
	) {
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn(locale);
		when(ragGroundingService.retrieve(any(UUID.class), any(UUID.class), any(String.class), eq(locale), any(AgentCommandMode.class)))
				.thenReturn(ragContext);
		ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
		when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
		ObjectProvider<com.bubli.agent.model.AiCallExecutor> aiCallExecutorProvider = mock(ObjectProvider.class);
		when(aiCallExecutorProvider.getIfAvailable()).thenReturn(null);
		return new ProjectRoomAgentCommandService(
				mock(ProjectMembershipPublicService.class),
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				userLocalePublicService,
				resourcePublicService,
				ragGroundingService,
				chatModelProvider,
				aiCallExecutorProvider,
				new ObjectMapper()
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

	private ResourceSearchHit hit(UUID resourceId, String chunkText, double similarityScore) {
		return new ResourceSearchHit(
				UUID.randomUUID(),
				resourceId,
				0,
				chunkText,
				2,
				"{\"pageNumber\":2}",
				similarityScore
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
				ResourceStatus.ANALYZED,
				Instant.parse("2026-07-01T01:00:00Z"),
				Instant.parse("2026-07-01T01:00:00Z")
		);
	}
}
