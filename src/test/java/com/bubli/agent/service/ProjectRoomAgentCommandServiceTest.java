package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.ProjectRoomGroundingContext;
import com.bubli.agent.dto.ProjectRoomGroundingEvidence;
import com.bubli.agent.dto.ProjectRoomGroundingSourceType;
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
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRoomAgentCommandServiceTest {

	@Test
	void returnsNoAnswerWithoutCallingLlmWhenGroundingContextIsEmpty() {
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
				ProjectRoomGroundingContext.ungrounded(),
				chatModel
		).execute(userId, roomId, "/bubli 내 ID가 뭐야", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
		assertThat(response.message().body().get("text").asText())
				.isEqualTo("프로젝트 문서 및 관리 데이터 기준에서는 알 수 없는 내용입니다.");
		assertThat(response.message().body().get("grounded").asBoolean()).isFalse();
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
				ProjectRoomGroundingContext.ungrounded(),
				mock(ChatModel.class)
		).execute(userId, roomId, "/bubli 内容を教えて", AgentCommandMode.ANSWER, List.of());

		assertThat(response.message().body().get("text").asText())
				.isEqualTo("プロジェクト資料および管理データの範囲では分かりません。");
	}

	@Test
	void groundedAnswerPromptUsesOnlyRetrievedProjectSources() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomGroundingContext context = documentContext(
				resourceId,
				"契約期間は2026年7月1日から2026年9月30日までです。"
		);

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
				context,
				chatModel
		).execute(userId, roomId, "/bubli 契約期間は?", AgentCommandMode.ANSWER, List.of());

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue()).contains("Use ONLY the project documents and management data");
		assertThat(promptCaptor.getValue()).contains("契約期間は2026年7月1日から2026年9月30日までです。");
		assertThat(promptCaptor.getValue()).doesNotContain("Recent room chat");
		assertThat(promptCaptor.getValue()).doesNotContain("Room memory summaries");
		assertThat(response.message().resourceId()).isEqualTo(resourceId);
		assertThat(response.message().body().get("grounded").asBoolean()).isTrue();
		assertThat(response.message().body().get("sourceTypes").get(0).asText()).isEqualTo("DOCUMENT");
		assertThat(response.message().body().get("ragHits").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@Test
	void suggestModeWithoutGroundingDoesNotCreateSuggestion() {
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
				ProjectRoomGroundingContext.ungrounded(),
				chatModel
		).execute(userId, roomId, "/bubli todo 만들어줘", AgentCommandMode.SUGGEST, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(suggestionCommandService, never()).createDraft(any(), any(), any(), any(), any(), any(), any());
		assertThat(response.suggestions()).isEmpty();
	}

	@Test
	void suggestModeWithScheduleGroundingCreatesTodoDraft() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID scheduleId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ProjectRoomGroundingContext context = managementContext(
				ProjectRoomGroundingSourceType.SCHEDULE,
				scheduleId,
				"[SCHEDULE]\nscheduleId=%s\ntitle=検収会議\nstartsAt=2026-07-07T01:00:00Z".formatted(scheduleId)
		);

		when(chatModel.call(any(String.class))).thenReturn("TODO: 検収会議の準備を行う。");
		when(suggestionCommandService.createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(null),
				eq(AgentSuggestionType.TODO),
				any(),
				any()
		)).thenReturn(suggestionResponse(suggestionId, userId, roomId, null, AgentSuggestionType.TODO));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				"ja-JP",
				context,
				chatModel
		).execute(userId, roomId, "/bubli 今週の予定を基にTODOを作って", AgentCommandMode.SUGGEST, List.of());

		verify(suggestionCommandService).createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(null),
				eq(AgentSuggestionType.TODO),
				any(),
				org.mockito.ArgumentMatchers.assertArg(evidence -> {
					assertThat(evidence.get("sourceTypes")).isEqualTo(List.of("SCHEDULE"));
					assertThat(evidence.get("scheduleIds")).isEqualTo(List.of(scheduleId));
				})
		);
		verify(eventPublicService).recordAgentSuggestionsCreated(
				userId,
				roomId,
				List.of(suggestionId),
				List.of(AgentSuggestionType.TODO.name())
		);
		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.message().body().get("scheduleIds").get(0).asText()).isEqualTo(scheduleId.toString());
	}

	@Test
	void suggestModeWithAgentSuggestionGroundingCreatesWbsDraft() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sourceSuggestionId = UUID.randomUUID();
		UUID createdSuggestionId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		AgentSuggestionCommandService suggestionCommandService = mock(AgentSuggestionCommandService.class);
		ChatMessagePublicService chatMessagePublicService = mock(ChatMessagePublicService.class);
		RoomMemoryPublicService memoryPublicService = mock(RoomMemoryPublicService.class);
		ProjectRoomEventPublicService eventPublicService = mock(ProjectRoomEventPublicService.class);
		ProjectRoomGroundingContext context = managementContext(
				ProjectRoomGroundingSourceType.AGENT_SUGGESTION,
				sourceSuggestionId,
				"[AGENT_SUGGESTION]\nsuggestionId=%s\ntype=TODO\npayload={title=画面設計}".formatted(sourceSuggestionId)
		);

		when(chatModel.call(any(String.class))).thenReturn("WBS: 画面設計を分解します。");
		when(suggestionCommandService.createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(null),
				eq(AgentSuggestionType.WBS),
				any(),
				any()
		)).thenReturn(suggestionResponse(createdSuggestionId, userId, roomId, null, AgentSuggestionType.WBS));
		when(chatMessagePublicService.createRoomAgentResponse(eq(userId), eq(roomId), any(), eq(null)))
				.thenAnswer(invocation -> chatMessage(invocation.getArgument(2), null));
		when(memoryPublicService.createDraft(eq(userId), eq(roomId), eq(10L), eq(10L), any()))
				.thenReturn(memory());

		var response = service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				"ja-JP",
				context,
				chatModel
		).execute(userId, roomId, "/bubli AI候補を見てWBSで整理して", AgentCommandMode.SUGGEST, List.of());

		verify(suggestionCommandService).createDraft(
				eq(userId),
				eq(roomId),
				eq(null),
				eq(null),
				eq(AgentSuggestionType.WBS),
				any(),
				org.mockito.ArgumentMatchers.assertArg(evidence -> {
					assertThat(evidence.get("sourceTypes")).isEqualTo(List.of("AGENT_SUGGESTION"));
					assertThat(evidence.get("agentSuggestionIds")).isEqualTo(List.of(sourceSuggestionId));
				})
		);
		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.message().body().get("agentSuggestionIds").get(0).asText())
				.isEqualTo(sourceSuggestionId.toString());
	}

	@Test
	void resourceInventoryQuestionReturnsUploadedResourcesWithoutGroundingOrLlm() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ProjectRoomGroundingService groundingService = mock(ProjectRoomGroundingService.class);
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
				ProjectRoomGroundingContext.ungrounded(),
				chatModel,
				resourcePublicService,
				groundingService
		).execute(userId, roomId, "/bubli 현재 프로젝트에 업로드된 파일이 뭐야?", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(groundingService, never()).retrieve(any(), any(), any(), any(), any());
		assertThat(response.message().body().get("text").asText()).contains("契約書.pdf");
		assertThat(response.message().body().get("resources").get(0).get("resourceId").asText())
				.isEqualTo(resourceId.toString());
	}

	@Test
	void japaneseResourceInventoryQuestionReturnsUploadedResourcesWithoutGroundingOrLlm() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ProjectRoomGroundingService groundingService = mock(ProjectRoomGroundingService.class);
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
				ProjectRoomGroundingContext.ungrounded(),
				chatModel,
				resourcePublicService,
				groundingService
		).execute(userId, roomId, "/bubli どんなファイルがある？", AgentCommandMode.ANSWER, List.of());

		verify(chatModel, never()).call(any(String.class));
		verify(groundingService, never()).retrieve(any(), any(), any(), any(), any());
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
			ProjectRoomGroundingContext groundingContext,
			ChatModel chatModel
	) {
		ProjectRoomGroundingService groundingService = mock(ProjectRoomGroundingService.class);
		return service(
				chatMessagePublicService,
				memoryPublicService,
				suggestionCommandService,
				eventPublicService,
				locale,
				groundingContext,
				chatModel,
				mock(ResourcePublicService.class),
				groundingService
		);
	}

	@SuppressWarnings("unchecked")
	private ProjectRoomAgentCommandService service(
			ChatMessagePublicService chatMessagePublicService,
			RoomMemoryPublicService memoryPublicService,
			AgentSuggestionCommandService suggestionCommandService,
			ProjectRoomEventPublicService eventPublicService,
			String locale,
			ProjectRoomGroundingContext groundingContext,
			ChatModel chatModel,
			ResourcePublicService resourcePublicService,
			ProjectRoomGroundingService groundingService
	) {
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn(locale);
		when(groundingService.retrieve(any(UUID.class), any(UUID.class), any(String.class), eq(locale), any(AgentCommandMode.class)))
				.thenReturn(groundingContext);
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
				groundingService,
				chatModelProvider,
				aiCallExecutorProvider,
				new ObjectMapper()
		);
	}

	private ProjectRoomGroundingContext documentContext(UUID resourceId, String chunkText) {
		ResourceSearchHit hit = hit(resourceId, chunkText, 0.93D);
		ProjectRoomGroundingEvidence evidence = new ProjectRoomGroundingEvidence(
				ProjectRoomGroundingSourceType.DOCUMENT,
				resourceId,
				Map.of("chunkIndex", 0, "pageNumber", 2, "similarityScore", 0.93D)
		);
		String promptBlock = """
				[DOCUMENT]
				resourceId=%s
				chunkIndex=0
				pageNumber=2
				similarityScore=0.93
				chunkText=
				%s
				""".formatted(resourceId, chunkText);
		return new ProjectRoomGroundingContext(true, List.of(hit), 0.93D, List.of(evidence), promptBlock);
	}

	private ProjectRoomGroundingContext managementContext(
			ProjectRoomGroundingSourceType sourceType,
			UUID sourceId,
			String promptBlock
	) {
		ProjectRoomGroundingEvidence evidence = new ProjectRoomGroundingEvidence(
				sourceType,
				sourceId,
				Map.of("title", "source")
		);
		return new ProjectRoomGroundingContext(true, List.of(), 0.0D, List.of(evidence), promptBlock);
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
