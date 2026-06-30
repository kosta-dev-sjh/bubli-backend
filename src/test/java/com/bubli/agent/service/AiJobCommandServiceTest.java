package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiJobCommandServiceTest {

	@Mock
	ResourcePublicService resourcePublicService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	AgentJobService agentJobService;

	@InjectMocks
	AiJobCommandService aiJobCommandService;

	@Test
	void createAnalyzeResourceJobChecksResourceAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(resourcePublicService.getReadableResource(userId, resourceId)).willReturn(new ResourceResult(
				resourceId,
				userId,
				roomId,
				"계약서",
				ResourceKind.FILE,
				ResourceVisibility.ROOM_SHARED,
				ResourceStatus.READY,
				Instant.now(),
				Instant.now()
		));
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						resourceId,
						AgentJobType.ANALYZE_RESOURCE,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createAnalyzeResourceJob(userId, resourceId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.ANALYZE_RESOURCE);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isEqualTo(resourceId);
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.ANALYZE_RESOURCE);
	}

	@Test
	void createGenerateRequirementsJobChecksRoomAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.GENERATE_REQUIREMENTS,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createGenerateRequirementsJob(userId, roomId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.GENERATE_REQUIREMENTS);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isNull();
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.GENERATE_REQUIREMENTS);
	}

	@Test
	void createGenerateTasksJobChecksRoomAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.GENERATE_TASKS,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createGenerateTasksJob(userId, roomId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.GENERATE_TASKS);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isNull();
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.GENERATE_TASKS);
	}

	@Test
	void createGenerateWbsJobChecksRoomAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.GENERATE_WBS,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createGenerateWbsJob(userId, roomId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.GENERATE_WBS);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isNull();
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.GENERATE_WBS);
	}

	@Test
	void createGenerateQuestionsJobChecksRoomAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.GENERATE_QUESTIONS,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createGenerateQuestionsJob(userId, roomId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.GENERATE_QUESTIONS);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isNull();
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.GENERATE_QUESTIONS);
	}

	@Test
	void createReviewContractDocumentsJobChecksRoomAccessThenCreatesPendingAgentJob() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.REVIEW_CONTRACT_DOCUMENTS,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		AgentJobResult result = aiJobCommandService.createReviewContractDocumentsJob(userId, roomId);

		assertThat(result.id()).isEqualTo(jobId);
		assertThat(result.jobType()).isEqualTo(AgentJobType.REVIEW_CONTRACT_DOCUMENTS);
		assertThat(result.status()).isEqualTo(AgentJobStatus.PENDING);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().roomId()).isEqualTo(roomId);
		assertThat(commandCaptor.getValue().resourceId()).isNull();
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.REVIEW_CONTRACT_DOCUMENTS);
	}

	@Test
	void createDailySummaryJobStoresRequestedSummaryDatePayload() {
		UUID userId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						null,
						null,
						AgentJobType.DAILY_SUMMARY,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		aiJobCommandService.createDailySummaryJob(userId, LocalDate.of(2026, 7, 1), "Asia/Tokyo");

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.DAILY_SUMMARY);
		assertThat(commandCaptor.getValue().requestPayload()).containsEntry("summaryDate", "2026-07-01");
		assertThat(commandCaptor.getValue().requestPayload()).containsEntry("timezone", "Asia/Tokyo");
	}

	@Test
	void createDraftDocumentJobStoresDraftRequestPayload() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID sourceResourceId = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		given(agentJobService.create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any()))
				.willReturn(new AgentJobResult(
						jobId,
						userId,
						roomId,
						null,
						AgentJobType.DRAFT_DOCUMENT,
						AgentJobStatus.PENDING,
						0,
						null,
						null,
						null,
						null,
						Instant.now(),
						Instant.now()
				));

		aiJobCommandService.createDraftDocumentJob(
				userId,
				roomId,
				"proposal",
				List.of(sourceResourceId),
				"견적서 초안을 작성"
		);

		ArgumentCaptor<CreateAgentJobCommand> commandCaptor = ArgumentCaptor.forClass(CreateAgentJobCommand.class);
		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
		verify(agentJobService).create(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.DRAFT_DOCUMENT);
		assertThat(commandCaptor.getValue().requestPayload())
				.containsEntry("documentType", "proposal")
				.containsEntry("instruction", "견적서 초안을 작성");
		assertThat(commandCaptor.getValue().requestPayload().get("sourceResourceIds").toString())
				.contains(sourceResourceId.toString());
	}
}
