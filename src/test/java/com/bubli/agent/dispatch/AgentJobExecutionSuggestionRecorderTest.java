package com.bubli.agent.dispatch;

import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.service.AgentSuggestionService;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AgentJobExecutionSuggestionRecorderTest {

	@Test
	void recordSuggestionsCreatesDraftSuggestionsFromAgentJobContext() {
		AgentSuggestionService agentSuggestionService = mock(AgentSuggestionService.class);
		AgentJobExecutionSuggestionRecorder recorder = new AgentJobExecutionSuggestionRecorder(agentSuggestionService);
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(userId, roomId, resourceId, AgentJobType.GENERATE_TASKS);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		AgentJobExecutionSuggestionDraft draft = new AgentJobExecutionSuggestionDraft(
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"agent\"}"
		);

		int recordedCount = recorder.recordSuggestions(agentJob, List.of(draft));

		assertThat(recordedCount).isEqualTo(1);
		ArgumentCaptor<CreateAgentSuggestionCommand> commandCaptor =
				ArgumentCaptor.forClass(CreateAgentSuggestionCommand.class);
		verify(agentSuggestionService).createDraft(commandCaptor.capture());
		CreateAgentSuggestionCommand command = commandCaptor.getValue();
		assertThat(command.userId()).isEqualTo(userId);
		assertThat(command.roomId()).isEqualTo(roomId);
		assertThat(command.resourceId()).isEqualTo(resourceId);
		assertThat(command.jobId()).isEqualTo(jobId);
		assertThat(command.suggestionType()).isEqualTo(AgentSuggestionType.TODO);
		assertThat(command.payloadJson()).isEqualTo("{\"title\":\"시안 정리\"}");
		assertThat(command.evidenceJson()).isEqualTo("{\"source\":\"agent\"}");
	}

	@Test
	void recordSuggestionsReturnsZeroWhenDraftsAreEmpty() {
		AgentSuggestionService agentSuggestionService = mock(AgentSuggestionService.class);
		AgentJobExecutionSuggestionRecorder recorder = new AgentJobExecutionSuggestionRecorder(agentSuggestionService);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);

		int recordedCount = recorder.recordSuggestions(agentJob, List.of());

		assertThat(recordedCount).isZero();
		verifyNoInteractions(agentSuggestionService);
	}
}
