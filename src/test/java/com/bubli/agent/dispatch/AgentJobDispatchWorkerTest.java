package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AgentJobDispatchWorkerTest {

	@Test
	void processNextQueuedJobReturnsFalseWhenQueueIsEmpty() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		when(queueConsumer.poll()).thenReturn(Optional.empty());

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isFalse();
		verifyNoInteractions(
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
	}

	@Test
	void processNextQueuedJobMarksPendingJobRunningAndStoresStartedEvent() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJobQueueMessage message = message(jobId);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		when(queueConsumer.poll()).thenReturn(Optional.of(message));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		when(executionPort.execute(message)).thenReturn(Optional.empty());

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isTrue();
		assertThat(agentJob.getStatus()).isEqualTo(AgentJobStatus.RUNNING);
		assertThat(agentJob.getStartedAt()).isNotNull();
		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(agentJobEventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getJobId()).isEqualTo(jobId);
		assertThat(eventCaptor.getValue().getEventType())
				.isEqualTo(AgentJobDispatchWorker.STARTED_EVENT_TYPE);
		assertThat(eventCaptor.getValue().getMessage())
				.isEqualTo(AgentJobDispatchWorker.STARTED_EVENT_MESSAGE);
		verifyNoInteractions(executionResultRecorder, suggestionRecorder);
	}

	@Test
	void processNextQueuedJobRecordsSucceededOutcome() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJobQueueMessage message = message(jobId);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		when(queueConsumer.poll()).thenReturn(Optional.of(message));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		when(executionPort.execute(message)).thenReturn(Optional.of(AgentJobExecutionOutcome.succeeded()));

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isTrue();
		verify(executionResultRecorder).recordSucceeded(jobId);
		verify(executionResultRecorder, never()).recordFailed(any(), any(), any());
		verifyNoInteractions(suggestionRecorder);
	}

	@Test
	void processNextQueuedJobRecordsSuggestionsBeforeSucceededOutcome() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJobQueueMessage message = message(jobId);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		AgentJobExecutionSuggestionDraft draft = new AgentJobExecutionSuggestionDraft(
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"agent\"}"
		);
		when(queueConsumer.poll()).thenReturn(Optional.of(message));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		when(executionPort.execute(message))
				.thenReturn(Optional.of(AgentJobExecutionOutcome.succeededWithSuggestions(List.of(draft))));

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isTrue();
		verify(suggestionRecorder).recordSuggestions(agentJob, List.of(draft));
		verify(executionResultRecorder).recordSucceeded(jobId);
	}

	@Test
	void processNextQueuedJobMarksFailedWhenSuggestionRecordingFails() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJobQueueMessage message = message(jobId);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.GENERATE_TASKS
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		AgentJobExecutionSuggestionDraft draft = new AgentJobExecutionSuggestionDraft(
				AgentSuggestionType.TODO,
				"{\"title\":\"시안 정리\"}",
				"{\"source\":\"agent\"}"
		);
		when(queueConsumer.poll()).thenReturn(Optional.of(message));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		when(executionPort.execute(message))
				.thenReturn(Optional.of(AgentJobExecutionOutcome.succeededWithSuggestions(List.of(draft))));
		doThrow(new IllegalStateException("suggestion write failed"))
				.when(suggestionRecorder)
				.recordSuggestions(eq(agentJob), eq(List.of(draft)));

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isTrue();
		verify(executionResultRecorder, never()).recordSucceeded(any());
		verify(executionResultRecorder).recordFailed(
				jobId,
				"AGENT_SUGGESTION_RECORD_FAILED",
				"suggestion write failed"
		);
	}

	@Test
	void processNextQueuedJobRecordsFailedOutcome() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJobQueueMessage message = message(jobId);
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		when(queueConsumer.poll()).thenReturn(Optional.of(message));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));
		when(executionPort.execute(message))
				.thenReturn(Optional.of(AgentJobExecutionOutcome.failed("MODEL_TIMEOUT", "모델 응답 시간이 초과되었습니다.")));

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isTrue();
		verify(executionResultRecorder, never()).recordSucceeded(any());
		verify(executionResultRecorder).recordFailed(jobId, "MODEL_TIMEOUT", "모델 응답 시간이 초과되었습니다.");
		verifyNoInteractions(suggestionRecorder);
	}

	@Test
	void processNextQueuedJobIgnoresMissingJob() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		when(queueConsumer.poll()).thenReturn(Optional.of(message(jobId)));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.empty());

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isFalse();
		verify(agentJobRepository).findById(jobId);
		verify(agentJobEventRepository, never()).save(any());
		verifyNoInteractions(executionPort, executionResultRecorder, suggestionRecorder);
	}

	@Test
	void processNextQueuedJobIgnoresAlreadyRunningJob() {
		AgentJobQueueConsumerPort queueConsumer = mock(AgentJobQueueConsumerPort.class);
		AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
		AgentJobEventRepository agentJobEventRepository = mock(AgentJobEventRepository.class);
		AgentJobExecutionPort executionPort = mock(AgentJobExecutionPort.class);
		AgentJobExecutionResultRecorder executionResultRecorder = mock(AgentJobExecutionResultRecorder.class);
		AgentJobExecutionSuggestionRecorder suggestionRecorder = mock(AgentJobExecutionSuggestionRecorder.class);
		AgentJobDispatchWorker worker = new AgentJobDispatchWorker(
				queueConsumer,
				agentJobRepository,
				agentJobEventRepository,
				executionPort,
				executionResultRecorder,
				suggestionRecorder
		);
		UUID jobId = UUID.randomUUID();
		AgentJob agentJob = AgentJob.create(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		);
		ReflectionTestUtils.setField(agentJob, "id", jobId);
		agentJob.markRunning();
		when(queueConsumer.poll()).thenReturn(Optional.of(message(jobId)));
		when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(agentJob));

		boolean processed = worker.processNextQueuedJob();

		assertThat(processed).isFalse();
		verify(agentJobEventRepository, never()).save(any());
		verifyNoInteractions(executionPort, executionResultRecorder, suggestionRecorder);
	}

	private AgentJobQueueMessage message(UUID jobId) {
		return new AgentJobQueueMessage(
				jobId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE,
				Instant.now()
		);
	}
}
