package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.AgentSuggestionResult;
import com.bubli.agent.dto.AiDocumentResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.dto.CreateAiDocumentCommand;
import com.bubli.agent.dto.UpdateAgentSuggestionCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.repository.AiDocumentRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentStorageServiceTest {

	@Mock
	AgentJobRepository agentJobRepository;

	@Mock
	AgentJobEventRepository agentJobEventRepository;

	@Mock
	AgentSuggestionRepository agentSuggestionRepository;

	@Mock
	AiDocumentRepository aiDocumentRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

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
	void getRequestedJobReturnsOnlyRequesterJob() {
		UUID userId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				userId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		given(agentJobRepository.findByIdAndRequestedByUserId(jobId, userId)).willReturn(Optional.of(agentJob));

		AgentJobResult result = agentJobService.getRequestedJob(userId, jobId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.requestedByUserId()).isEqualTo(userId);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);
	}

	@Test
	void getRequestedJobThrowsAgentNotFoundWhenRequesterDoesNotOwnJob() {
		UUID userId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobRepository.findByIdAndRequestedByUserId(jobId, userId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> agentJobService.getRequestedJob(userId, jobId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AGENT_404_001));
	}

	@Test
	void getRequestedJobEventsReturnsEventsAfterRequesterCheck() {
		UUID userId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				userId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		AgentJobEvent event = AgentJobEvent.create(jobId, "STARTED", "분석을 시작했습니다.");
		ReflectionTestUtils.setField(event, "id", eventId);
		PageRequest pageable = PageRequest.of(0, 20);
		given(agentJobRepository.findByIdAndRequestedByUserId(jobId, userId)).willReturn(Optional.of(agentJob));
		given(agentJobEventRepository.findByJobId(eq(jobId), any(Pageable.class)))
				.willReturn(new PageImpl<>(List.of(event), pageable, 1));

		var result = agentJobService.getRequestedJobEvents(userId, jobId, pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().id()).isEqualTo(eventId);
		assertThat(result.getItems().getFirst().eventType()).isEqualTo("STARTED");
		assertThat(result.getItems().getFirst().message()).isEqualTo("분석을 시작했습니다.");
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
				AgentSuggestionType.TODO,
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
	void getPersonalSuggestionsReturnsUserSuggestionsByStatus() {
		UUID userId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				userId,
				null,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"resource\"}"
		);
		ReflectionTestUtils.setField(suggestion, "id", suggestionId);
		PageRequest pageable = PageRequest.of(0, 20);
		given(agentSuggestionRepository.findByUserIdAndStatus(
				eq(userId),
				eq(AgentSuggestionStatus.DRAFT),
				any(Pageable.class)
		)).willReturn(new PageImpl<>(List.of(suggestion), pageable, 1));

		var result = agentSuggestionService.getPersonalSuggestions(userId, AgentSuggestionStatus.DRAFT, pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().id()).isEqualTo(suggestionId);
		assertThat(result.getItems().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.TODO);
		assertThat(result.getItems().getFirst().status()).isEqualTo(AgentSuggestionStatus.DRAFT);
	}

	@Test
	void getRoomSuggestionsRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				userId,
				roomId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentSuggestionType.QUESTION,
				"{\"question\":\"확인할까요?\"}",
				null
		);
		ReflectionTestUtils.setField(suggestion, "id", suggestionId);
		PageRequest pageable = PageRequest.of(0, 20);
		given(agentSuggestionRepository.findByRoomIdAndStatus(
				eq(roomId),
				eq(AgentSuggestionStatus.DRAFT),
				any(Pageable.class)
		)).willReturn(new PageImpl<>(List.of(suggestion), pageable, 1));

		var result = agentSuggestionService.getRoomSuggestions(userId, roomId, AgentSuggestionStatus.DRAFT, pageable);

		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().getFirst().id()).isEqualTo(suggestionId);
		assertThat(result.getItems().getFirst().roomId()).isEqualTo(roomId);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
	}

	@Test
	void updatePersonalSuggestionChangesCandidateOnly() {
		UUID userId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				userId,
				null,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"resource\"}"
		);
		ReflectionTestUtils.setField(suggestion, "id", suggestionId);
		given(agentSuggestionRepository.findById(suggestionId)).willReturn(Optional.of(suggestion));

		AgentSuggestionResult result = agentSuggestionService.updateSuggestion(
				userId,
				suggestionId,
				new UpdateAgentSuggestionCommand(
						AgentSuggestionStatus.HELD,
						"{\"title\":\"시안 정리\",\"memo\":\"우선순위 재검토\"}",
						null
				)
		);

		assertThat(result.status()).isEqualTo(AgentSuggestionStatus.HELD);
		assertThat(result.payloadJson()).contains("우선순위 재검토");
		assertThat(result.evidenceJson()).contains("resource");
	}

	@Test
	void updateRoomSuggestionRequiresActiveRoomMember() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				UUID.randomUUID(),
				roomId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentSuggestionType.QUESTION,
				"{\"question\":\"확인할까요?\"}",
				null
		);
		ReflectionTestUtils.setField(suggestion, "id", suggestionId);
		given(agentSuggestionRepository.findById(suggestionId)).willReturn(Optional.of(suggestion));

		AgentSuggestionResult result = agentSuggestionService.updateSuggestion(
				userId,
				suggestionId,
				new UpdateAgentSuggestionCommand(AgentSuggestionStatus.REJECTED, null, null)
		);

		assertThat(result.status()).isEqualTo(AgentSuggestionStatus.REJECTED);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
	}

	@Test
	void updatePersonalSuggestionThrowsNotFoundForOtherUser() {
		UUID ownerId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID suggestionId = UUID.randomUUID();
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				ownerId,
				null,
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				null
		);
		ReflectionTestUtils.setField(suggestion, "id", suggestionId);
		given(agentSuggestionRepository.findById(suggestionId)).willReturn(Optional.of(suggestion));

		assertThatThrownBy(() -> agentSuggestionService.updateSuggestion(
				otherUserId,
				suggestionId,
				new UpdateAgentSuggestionCommand(AgentSuggestionStatus.APPROVED, null, null)
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AGENT_404_002));
	}

	@Test
	void updateSuggestionRejectsEmptyPatch() {
		assertThatThrownBy(() -> agentSuggestionService.updateSuggestion(
				UUID.randomUUID(),
				UUID.randomUUID(),
				new UpdateAgentSuggestionCommand(null, null, null)
		)).isInstanceOfSatisfying(BusinessException.class, exception ->
				assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AGENT_400_001));
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

	@Test
	void getAiDocumentByResourceIdReturnsDocumentStatus() {
		UUID aiDocumentId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		AiDocument aiDocument = AiDocument.create(
				resourceId,
				roomId,
				AiDocumentType.CONTRACT,
				BigDecimal.valueOf(0.9500)
		);
		ReflectionTestUtils.setField(aiDocument, "id", aiDocumentId);
		given(aiDocumentRepository.findByResourceId(resourceId)).willReturn(Optional.of(aiDocument));

		AiDocumentResult result = aiDocumentService.getByResourceId(resourceId);

		assertThat(result.id()).isEqualTo(aiDocumentId);
		assertThat(result.resourceId()).isEqualTo(resourceId);
		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.documentType()).isEqualTo(AiDocumentType.CONTRACT);
		assertThat(result.status()).isEqualTo(AiDocumentStatus.READY);
	}

	@Test
	void getAiDocumentByResourceIdThrowsWhenMissing() {
		UUID resourceId = UUID.randomUUID();
		given(aiDocumentRepository.findByResourceId(resourceId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> aiDocumentService.getByResourceId(resourceId))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AGENT_404_003));
	}
}
