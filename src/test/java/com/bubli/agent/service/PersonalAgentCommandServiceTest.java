package com.bubli.agent.service;

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
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
		assertThat(promptCaptor.getValue()).contains("Personal TODOs");
		assertThat(promptCaptor.getValue()).contains("Personal schedules");
		assertThat(promptCaptor.getValue()).contains("Personal memos");
		assertThat(promptCaptor.getValue()).contains("Personal resource summaries");
		assertThat(promptCaptor.getValue()).contains("Selected personal resources");
		assertThat(response.message().body().get("text").asText()).isEqualTo("LLM answer");
		assertThat(response.suggestions()).isEmpty();
	}

	@SuppressWarnings("unchecked")
	private PersonalAgentCommandService service(UUID userId, ChatModel chatModel) {
		TaskPublicService taskPublicService = mock(TaskPublicService.class);
		SchedulePublicService schedulePublicService = mock(SchedulePublicService.class);
		MemoPublicService memoPublicService = mock(MemoPublicService.class);
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		UserLocalePublicService userLocalePublicService = mock(UserLocalePublicService.class);
		ObjectProvider<ChatModel> chatModelProvider = mock(ObjectProvider.class);
		ObjectProvider<AiCallExecutor> aiCallExecutorProvider = mock(ObjectProvider.class);

		when(taskPublicService.getPersonalContextTasks(userId, 8)).thenReturn(List.of(task(userId)));
		when(schedulePublicService.getSchedulesBetween(any(), any(), any())).thenReturn(List.of(schedule(userId)));
		when(memoPublicService.getUpdatedMemosBetween(any(), any(), any(), anyInt()))
				.thenReturn(List.of(memo(userId)));
		when(resourcePublicService.getRecentAnalysisSummaries(userId, 5)).thenReturn(List.of(resourceSummary()));
		when(resourcePublicService.getReadableResource(any(), any())).thenReturn(resource(userId));
		when(userLocalePublicService.resolveLocaleCode(any(UUID.class), any())).thenReturn("ko-KR");
		when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
		when(aiCallExecutorProvider.getIfAvailable()).thenReturn(null);

		return new PersonalAgentCommandService(
				taskPublicService,
				schedulePublicService,
				memoPublicService,
				resourcePublicService,
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
		return new TaskResult(
				UUID.randomUUID(),
				userId,
				null,
				null,
				null,
				"Review memo",
				"Check local agent notes",
				TaskStatus.TODO,
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

	private ResourceResult resource(UUID userId) {
		return new ResourceResult(
				UUID.randomUUID(),
				userId,
				null,
				"Selected personal file",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.ANALYZED,
				Instant.now(),
				Instant.now()
		);
	}
}
