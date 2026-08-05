package com.bubli.agent.service;

import com.bubli.agent.config.AgentRagProperties;
import com.bubli.agent.dto.PersonalAgentMemoryMessage;
import com.bubli.agent.dto.PersonalAgentMemoryInput;
import com.bubli.agent.dto.PersonalAgentMemorySummary;
import com.bubli.agent.model.AiCallExecutor;
import com.bubli.agent.type.AgentCommandMode;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.chat.type.MessageType;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.service.MemoPublicService;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.service.ResourceSearchMetricsPublicService;
import com.bubli.resource.service.ResourceSemanticSearchPublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceSearchScope;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceSummaryStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.user.service.UserLocalePublicService;
import com.bubli.work.schedule.dto.ScheduleResult;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.schedule.type.ScheduleSyncStatus;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.task.type.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalAgentCommandServiceTest {

	@Test
	void suggestModeReturnsLocalTodoSuggestionWithoutServerPersistenceId() {
		UUID userId = UUID.randomUUID();
		PersonalAgentCommandService service = service(userId, null);

		var response = service.execute(
				userId,
				"Create a TODO for reviewing today's memo",
				AgentCommandMode.SUGGEST,
				List.of(),
				memory()
		);

		assertThat(response.message().senderType()).isEqualTo("AGENT");
		assertThat(response.message().messageType()).isEqualTo(MessageType.AGENT_RESPONSE);
		assertThat(response.message().body().get("serverPersisted").asBoolean()).isFalse();
		assertThat(response.message().body().get("localSuggestionCount").asInt()).isEqualTo(1);
		assertThat(response.suggestions()).hasSize(1);
		assertThat(response.suggestions().getFirst().localSuggestionId()).isNull();
		assertThat(response.suggestions().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.TODO);
		assertThat(response.suggestions().getFirst().payload()).containsEntry("status", TaskStatus.TODO.name());
		assertThat(response.suggestions().getFirst().evidence()).containsEntry("serverPersisted", false);
	}

	@Test
	void promptIncludesLocalMemoryAndPersonalContextWhenLlmIsAvailable() {
		UUID userId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(String.class))).thenReturn("LLM answer");
		PersonalAgentCommandService service = service(userId, chatModel);

		var response = service.execute(
				userId,
				"What should I do next?",
				AgentCommandMode.ANSWER,
				List.of(UUID.randomUUID()),
				memory()
		);

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue()).contains("Local chat summaries");
		assertThat(promptCaptor.getValue()).contains("previous local summary");
		assertThat(promptCaptor.getValue()).contains("Local recent messages");
		assertThat(promptCaptor.getValue()).contains("Personal active TODOs");
		assertThat(promptCaptor.getValue()).contains("Personal schedules");
		assertThat(promptCaptor.getValue()).contains("Personal memos");
		assertThat(promptCaptor.getValue()).contains("Personal resource summaries");
		assertThat(promptCaptor.getValue()).contains("Selected personal resources");
		assertThat(response.message().body().get("text").asText()).isEqualTo("LLM answer");
		assertThat(response.suggestions()).isEmpty();
	}

	@Test
	void completedTaskQuestionPlacesCompletedTasksFirst() {
		UUID userId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(String.class))).thenReturn("완료된 작업을 기준으로 정리했습니다.");
		PersonalAgentCommandService service = service(userId, chatModel);

		var response = service.execute(
				userId,
				"완료된 작업 기준으로 정리해줘",
				AgentCommandMode.ANSWER,
				List.of(),
				memory()
		);

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue().indexOf("Personal completed TODOs"))
				.isLessThan(promptCaptor.getValue().indexOf("Personal active TODOs"));
		assertThat(response.message().body().get("matchedTasks").get(0).get("workState").asText())
				.isEqualTo("COMPLETED");
	}

	@Test
	void personalSuggestModeNeverCreatesWbsSuggestions() {
		UUID userId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(String.class))).thenReturn("""
				- 자료 검토
				- 클라이언트 질문 정리
				""");
		PersonalAgentCommandService service = service(userId, chatModel);

		var response = service.execute(
				userId,
				"WBS로 정리해줘",
				AgentCommandMode.SUGGEST,
				List.of(),
				memory()
		);

		assertThat(response.suggestions()).hasSize(2);
		assertThat(response.suggestions())
				.allSatisfy(suggestion -> assertThat(suggestion.suggestionType()).isNotEqualTo(AgentSuggestionType.WBS));
	}

	@Test
	void promptIncludesPersonalResourceMatchedByTitleWhenLlmIsAvailable() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		when(chatModel.call(any(String.class))).thenReturn("LLM answer");
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ResourceResult resource = resource(userId, resourceId, "02-design-outsourcing-requirements-example.pdf");
		when(resourcePublicService.getRecentAnalysisSummaries(userId, 5)).thenReturn(List.of());
		when(resourcePublicService.getRecentPersonalResources(userId, 30)).thenReturn(List.of(resource));
		when(resourcePublicService.findResourceSummary(userId, resourceId))
				.thenReturn(Optional.of(resourceSummary(resourceId)));
		PersonalAgentCommandService service = service(userId, chatModel, resourcePublicService);

		service.execute(
				userId,
				"/bubli 02 design outsourcing pdf 주요 내용",
				AgentCommandMode.ANSWER,
				List.of(),
				memory()
		);

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue()).contains("Personal matched resource evidence");
		assertThat(promptCaptor.getValue()).contains("retrievalMode=TITLE_MATCH");
		assertThat(promptCaptor.getValue()).contains("02-design-outsourcing-requirements-example.pdf");
		assertThat(promptCaptor.getValue()).contains("브랜드 가이드");
	}

	@Test
	void personalDocumentSearchUsesConfiguredPersonalMinSimilarity() {
		UUID userId = UUID.randomUUID();
		UUID lowResourceId = UUID.randomUUID();
		UUID highResourceId = UUID.randomUUID();
		ChatModel chatModel = mock(ChatModel.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		ResourceSemanticSearchPublicService searchService = mock(ResourceSemanticSearchPublicService.class);

		when(chatModel.call(any(String.class))).thenReturn("LLM answer");
		when(resourcePublicService.getRecentPersonalResources(userId, 30)).thenReturn(List.of());
		when(searchService.search(
				eq(userId),
				eq(ResourceSearchScope.PERSONAL),
				isNull(),
				any(String.class),
				eq(5)
		)).thenReturn(List.of(
				hit(lowResourceId, "low confidence contract clause", "low.pdf", 0.84D),
				hit(highResourceId, "high confidence contract clause", "high.pdf", 0.91D)
		));

		PersonalAgentCommandService service = service(
				userId,
				chatModel,
				resourcePublicService,
				searchService,
				new AgentRagProperties(true, 5, 0.72D, 0.68D, 0.85D),
				List.of()
		);

		var response = service.execute(
				userId,
				"/bubli 계약서 내용 알려줘",
				AgentCommandMode.ANSWER,
				List.of(),
				memory()
		);

		var promptCaptor = forClass(String.class);
		verify(chatModel).call(promptCaptor.capture());
		assertThat(promptCaptor.getValue())
				.contains("high confidence contract clause")
				.doesNotContain("low confidence contract clause");
		assertThat(response.message().body().get("citations").size()).isEqualTo(1);
		assertThat(response.message().body().get("citations").get(0).get("title").asText())
				.isEqualTo("high.pdf");
	}

	@SuppressWarnings("unchecked")
	private PersonalAgentCommandService service(UUID userId, ChatModel chatModel) {
		return service(userId, chatModel, mock(ResourcePublicService.class));
	}

	@SuppressWarnings("unchecked")
	private PersonalAgentCommandService service(
			UUID userId,
			ChatModel chatModel,
			ResourcePublicService resourcePublicService
	) {
		return service(
				userId,
				chatModel,
				resourcePublicService,
				mock(ResourceSemanticSearchPublicService.class),
				new AgentRagProperties(true, 5, 0.72D, 0.68D, 0.72D),
				List.of(resourceSummary())
		);
	}

	@SuppressWarnings("unchecked")
	private PersonalAgentCommandService service(
			UUID userId,
			ChatModel chatModel,
			ResourcePublicService resourcePublicService,
			ResourceSemanticSearchPublicService resourceSemanticSearchService,
			AgentRagProperties agentRagProperties,
			List<ResourceAnalysisSummaryResult> analysisSummaries
	) {
		TaskPublicService taskPublicService = mock(TaskPublicService.class);
		SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
		MemoPublicService memoPublicService = mock(MemoPublicService.class);
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
		ObjectProvider<AiCallExecutor> aiCallExecutorProvider = mock(ObjectProvider.class);

		when(taskPublicService.getPersonalContextTasks(userId, 20))
				.thenReturn(List.of(task(userId), task(userId, TaskStatus.DONE)));
		when(schedulePublicService.getSchedulesBetween(any(), any(), any())).thenReturn(List.of(schedule(userId)));
		when(memoPublicService.getUpdatedMemosBetween(any(), any(), any(), anyInt()))
				.thenReturn(List.of(memo(userId)));
		when(resourcePublicService.getRecentAnalysisSummaries(userId, 5)).thenReturn(analysisSummaries);
		when(resourcePublicService.getReadableResource(any(), any())).thenReturn(resource(userId));
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn("ko-KR");
		when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
		when(aiCallExecutorProvider.getIfAvailable()).thenReturn(null);

		return new PersonalAgentCommandService(
				taskPublicService,
				schedulePublicService,
				memoPublicService,
				resourcePublicService,
				resourceSemanticSearchService,
				agentRagProperties,
				mock(ResourceSearchMetricsPublicService.class),
				userLocalePublicService,
				chatModelProvider,
				aiCallExecutorProvider,
				new com.fasterxml.jackson.databind.ObjectMapper()
		);
	}

	private PersonalAgentMemoryInput memory() {
		Instant now = Instant.parse("2026-07-07T10:00:00Z");
		return new PersonalAgentMemoryInput(
				List.of(new PersonalAgentMemoryMessage("USER", "remember this local message", now)),
				List.of(new PersonalAgentMemorySummary(
						"previous local summary",
						now.minusSeconds(3600),
						now
				))
		);
	}

	private TaskResult task(UUID userId) {
		return task(userId, TaskStatus.TODO);
	}

	private TaskResult task(UUID userId, TaskStatus status) {
		return new TaskResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				"Review memo",
				"Check local agent notes",
				status,
				null,
				Instant.now(),
				Instant.now()
		);
	}

	private ScheduleResult schedule(UUID userId) {
		return new ScheduleResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				null,
				null,
				null,
				"Personal meeting",
				Instant.now(),
				Instant.now().plusSeconds(3600),
				false,
				ScheduleSyncStatus.LOCAL_ONLY,
				null,
				Instant.now(),
				Instant.now()
		);
	}

	private MemoResult memo(UUID userId) {
		return new MemoResult(
				UUID.randomUUID(),
				userId,
				null,
				"Personal memo body",
				MemoStatus.ACTIVE,
				Instant.now(),
				Instant.now()
		);
	}

	private ResourceAnalysisSummaryResult resourceSummary() {
		return new ResourceAnalysisSummaryResult(
				UUID.randomUUID(),
				"Personal resource",
				"Resource summary",
				Instant.now()
		);
	}

	private ResourceSearchHit hit(UUID resourceId, String chunkText, String originalName, double similarityScore) {
		return new ResourceSearchHit(
				UUID.randomUUID(),
				resourceId,
				0,
				chunkText,
				2,
				10,
				12,
				120,
				260,
				originalName,
				"{\"pageNumber\":2,\"startLine\":10,\"endLine\":12,\"startOffset\":120,\"endOffset\":260}",
				similarityScore
		);
	}

	private ResourceResult resource(UUID userId) {
		return resource(userId, UUID.randomUUID(), "Selected personal file");
	}

	private ResourceResult resource(UUID userId, UUID resourceId, String title) {
		return new ResourceResult(
				resourceId,
				userId,
				null,
				title,
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.ANALYZED,
				Instant.now(),
				Instant.now()
		);
	}

	private ResourceSummaryResult resourceSummary(UUID resourceId) {
		return new ResourceSummaryResult(
				UUID.randomUUID(),
				resourceId,
				UUID.randomUUID(),
				"{\"summary\":\"브랜드 가이드, 화면 시안, 검수 일정 정리가 필요합니다.\"}",
				"[]",
				ResourceSummaryStatus.READY,
				"test-prompt",
				"analysis.v1",
				"test-model",
				Instant.now(),
				Instant.now()
		);
	}
}
