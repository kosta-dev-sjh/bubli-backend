package com.bubli.agent.dispatch;

import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.service.AgentSuggestionService;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.resource.dto.ResourceAnalysisPage;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.bubli.resource.type.DocumentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentResourceAnalysisCompletionRecorderTest {

	@Test
	void completeStoresResourceAnalysisAndSuggestionsTogether() {
		ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
		AgentSuggestionService agentSuggestionService = mock(AgentSuggestionService.class);
		AgentResourceAnalysisCompletionRecorder recorder =
				new AgentResourceAnalysisCompletionRecorder(resourceAnalysisService, agentSuggestionService);
		UUID jobId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		AgentJobQueueMessage message = new AgentJobQueueMessage(
				jobId,
				userId,
				roomId,
				resourceId,
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		);
		ResourceAnalysisSource source = new ResourceAnalysisSource(
				resourceId,
				roomId,
				"contract.txt",
				"text/plain",
				DocumentType.CONTRACT,
				List.of(new ResourceAnalysisPage(null, "계약서 본문")),
				"계약서 본문",
				1,
				6
		);
		Map<String, Object> analysisJson = Map.of("summary", "계약 분석");
		AgentJobExecutionSuggestionDraft draft = new AgentJobExecutionSuggestionDraft(
				AgentSuggestionType.REVIEW_ITEM,
				"{\"title\":\"검수 조건 확인\"}",
				"{\"source\":\"contract.txt\"}"
		);

		int count = recorder.complete(message, source, analysisJson, List.of(draft));

		assertThat(count).isEqualTo(1);
		verify(resourceAnalysisService).completeAnalysisForJob(source, jobId, analysisJson);
		ArgumentCaptor<CreateAgentSuggestionCommand> commandCaptor =
				ArgumentCaptor.forClass(CreateAgentSuggestionCommand.class);
		verify(agentSuggestionService).createDraft(commandCaptor.capture());
		CreateAgentSuggestionCommand command = commandCaptor.getValue();
		assertThat(command.userId()).isEqualTo(userId);
		assertThat(command.roomId()).isEqualTo(roomId);
		assertThat(command.jobId()).isEqualTo(jobId);
		assertThat(command.resourceId()).isEqualTo(resourceId);
		assertThat(command.suggestionType()).isEqualTo(AgentSuggestionType.REVIEW_ITEM);
		assertThat(command.payloadJson()).contains("검수 조건 확인");
	}
}
