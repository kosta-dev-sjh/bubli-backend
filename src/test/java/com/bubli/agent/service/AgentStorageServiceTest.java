package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.AgentSuggestionResult;
import com.bubli.agent.dto.AiDocumentResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.dto.CreateAiDocumentCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.repository.AiDocumentRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentStorageServiceTest {

	@Mock
	AgentJobRepository agentJobRepository;

	@Mock
	AgentSuggestionRepository agentSuggestionRepository;

	@Mock
	AiDocumentRepository aiDocumentRepository;

	@InjectMocks
	AgentJobService agentJobService;

	@InjectMocks
	AgentSuggestionService agentSuggestionService;

	@InjectMocks
	AiDocumentService aiDocumentService;

	@Test
	void createAgentJobStoresPendingJobOnly() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobRepository.save(any(AgentJob.class))).willAnswer(invocation -> {
			AgentJob agentJob = invocation.getArgument(0);
			ReflectionTestUtils.setField(agentJob, "id", jobId);
			return agentJob;
		});

		AgentJobResult result = agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				resourceId,
				AgentJobType.ANALYZE_RESOURCE
		));

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.requestedByUserId()).isEqualTo(userId);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);
		assertThat(result.retryCount()).isZero();

		ArgumentCaptor<AgentJob> jobCaptor = ArgumentCaptor.forClass(AgentJob.class);
		verify(agentJobRepository).save(jobCaptor.capture());
		assertThat(jobCaptor.getValue().getResourceId()).isEqualTo(resourceId);
		assertThat(jobCaptor.getValue().getRoomId()).isEqualTo(roomId);
	}

	@Test
	void markFailedUpdatesJobStatusWithoutCreatingTargetDomainData() {
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);
		given(agentJobRepository.findById(jobId)).willReturn(Optional.of(agentJob));

		AgentJobResult result = agentJobService.markFailed(jobId, "MODEL_TIMEOUT", "모델 응답 시간이 초과되었습니다.");

		assertThat(result.status()).isEqualTo(AgentJobStatus.FAILED);
		assertThat(result.errorCode()).isEqualTo("MODEL_TIMEOUT");
		assertThat(result.finishedAt()).isNotNull();
	}

	@Test
	void createDraftSuggestionStoresCandidateAsDraft() {
		UUID suggestionId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		given(agentSuggestionRepository.save(any(AgentSuggestion.class))).willAnswer(invocation -> {
			AgentSuggestion suggestion = invocation.getArgument(0);
			ReflectionTestUtils.setField(suggestion, "id", suggestionId);
			return suggestion;
		});

		AgentSuggestionResult result = agentSuggestionService.createDraft(new CreateAgentSuggestionCommand(
				userId,
				null,
				jobId,
				resourceId,
				AgentSuggestionType.TASK,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"resource\"}"
		));

		assertThat(result.id()).isEqualTo(suggestionId);
		assertThat(result.userId()).isEqualTo(userId);
		assertThat(result.resourceId()).isEqualTo(resourceId);
		assertThat(result.status()).isEqualTo(AgentSuggestionStatus.DRAFT);
		assertThat(result.payloadJson()).contains("시안 정리");
	}

	@Test
	void createAiDocumentStoresReadyDocumentClassification() {
		UUID aiDocumentId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(aiDocumentRepository.save(any(AiDocument.class))).willAnswer(invocation -> {
			AiDocument aiDocument = invocation.getArgument(0);
			ReflectionTestUtils.setField(aiDocument, "id", aiDocumentId);
			return aiDocument;
		});

		AiDocumentResult result = aiDocumentService.create(new CreateAiDocumentCommand(
				resourceId,
				roomId,
				AiDocumentType.REQUIREMENT,
				BigDecimal.valueOf(0.8750)
		));

		assertThat(result.id()).isEqualTo(aiDocumentId);
		assertThat(result.resourceId()).isEqualTo(resourceId);
		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.status()).isEqualTo(AiDocumentStatus.READY);
		assertThat(result.detectedConfidence()).isEqualByComparingTo("0.875");
	}
}
