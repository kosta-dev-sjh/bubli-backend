package com.bubli.localsync.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.service.AgentJobPublicService;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.localsync.dto.LocalFileAnalysisCommand;
import com.bubli.localsync.dto.LocalFileAnalysisKeySentence;
import com.bubli.resource.dto.ResourceExtractedTextResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.StoreResourceExtractedTextCommand;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalFileAnalysisServiceTest {

	@Test
	void returnsExistingIdempotentJobWithoutStoringTextOrCreatingNewJob() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		AgentJobPublicService agentJobPublicService = mock(AgentJobPublicService.class);
		LocalFileAnalysisService service = new LocalFileAnalysisService(resourcePublicService, agentJobPublicService);
		LocalFileAnalysisCommand command = command(resourceId);
		AgentJobResult existingJob = job(userId, resourceId, AgentJobStatus.FAILED, 3);

		when(resourcePublicService.getReadableResource(userId, resourceId)).thenReturn(resource(userId, resourceId));
		when(agentJobPublicService.findAnalyzeResourceJobByIdempotencyKey(eq(userId), any(String.class)))
				.thenReturn(existingJob);

		AgentJobResult result = service.requestAnalysis(userId, command);

		assertThat(result).isEqualTo(existingJob);
		verify(resourcePublicService, never()).storeExtractedText(any(), any());
		verify(resourcePublicService, never()).startAnalysis(any(), any());
		verify(agentJobPublicService, never()).createAnalyzeResourceJobResult(any(), any(), any(), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void createsNewJobWithStableIdempotencyPayloadWhenNoExistingJobExists() {
		UUID userId = UUID.randomUUID();
		UUID resourceId = UUID.randomUUID();
		UUID extractedTextId = UUID.randomUUID();
		ResourcePublicService resourcePublicService = mock(ResourcePublicService.class);
		AgentJobPublicService agentJobPublicService = mock(AgentJobPublicService.class);
		LocalFileAnalysisService service = new LocalFileAnalysisService(resourcePublicService, agentJobPublicService);
		LocalFileAnalysisCommand command = command(resourceId);
		AgentJobResult createdJob = job(userId, resourceId, AgentJobStatus.PENDING, 0);

		when(resourcePublicService.getReadableResource(userId, resourceId)).thenReturn(resource(userId, resourceId));
		when(agentJobPublicService.findAnalyzeResourceJobByIdempotencyKey(eq(userId), any(String.class)))
				.thenReturn(null);
		when(resourcePublicService.storeExtractedText(eq(userId), any(StoreResourceExtractedTextCommand.class)))
				.thenReturn(new ResourceExtractedTextResult(extractedTextId, resourceId, command.localFileId(), command.extractionMethod()));
		when(resourcePublicService.startAnalysis(userId, resourceId)).thenReturn(resource(userId, resourceId));
		when(agentJobPublicService.createAnalyzeResourceJobResult(eq(userId), eq(null), eq(resourceId), any(Map.class)))
				.thenReturn(createdJob);

		AgentJobResult result = service.requestAnalysis(userId, command);

		assertThat(result).isEqualTo(createdJob);
		verify(agentJobPublicService).findAnalyzeResourceJobByIdempotencyKey(
				eq(userId),
				org.mockito.ArgumentMatchers.assertArg(key -> assertThat(key).startsWith("LOCAL_FILE_ANALYSIS:"))
		);
		verify(agentJobPublicService).createAnalyzeResourceJobResult(
				eq(userId),
				eq(null),
				eq(resourceId),
				org.mockito.ArgumentMatchers.assertArg(payload -> {
					assertThat(payload).containsEntry("source", "LOCAL_MANAGED_FOLDER_KEY_SENTENCES");
					assertThat(payload).containsEntry("analysisVersion", "v1");
					assertThat(payload.get("idempotencyKey").toString()).startsWith("LOCAL_FILE_ANALYSIS:");
					assertThat(payload).containsEntry("extractedTextId", extractedTextId.toString());
				})
		);
	}

	private LocalFileAnalysisCommand command(UUID resourceId) {
		return new LocalFileAnalysisCommand(
				resourceId,
				"local-file-1",
				"contract.pdf",
				"application/pdf",
				"checksum-1",
				"tauri-key-sentence-v1",
				100,
				80,
				List.of(new LocalFileAnalysisKeySentence(0, 0.95D, 0, 10, "important sentence")),
				"important sentence",
				false
		);
	}

	private ResourceResult resource(UUID userId, UUID resourceId) {
		return new ResourceResult(
				resourceId,
				userId,
				null,
				"contract.pdf",
				ResourceKind.FILE,
				ResourceVisibility.PERSONAL,
				ResourceStatus.READY,
				Instant.now(),
				Instant.now()
		);
	}

	private AgentJobResult job(UUID userId, UUID resourceId, AgentJobStatus status, int retryCount) {
		return new AgentJobResult(
				UUID.randomUUID(),
				userId,
				null,
				resourceId,
				AgentJobType.ANALYZE_RESOURCE,
				status,
				retryCount,
				null,
				null,
				null,
				null,
				Instant.now(),
				Instant.now()
		);
	}
}
