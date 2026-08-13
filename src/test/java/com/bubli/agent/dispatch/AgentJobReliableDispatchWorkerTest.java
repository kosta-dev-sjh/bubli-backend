package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobReliableDispatchWorkerTest {

	private AgentJobQueueConsumerPort queueConsumer;
	private AgentJobRepository jobRepository;
	private AgentJobEventRepository eventRepository;
	private AgentJobExecutionPort executionPort;
	private AgentJobExecutionResultRecorder resultRecorder;
	private AgentJobDispatchWorker worker;

	@BeforeEach
	void setUp() {
		queueConsumer = mock(AgentJobQueueConsumerPort.class);
		jobRepository = mock(AgentJobRepository.class);
		eventRepository = mock(AgentJobEventRepository.class);
		executionPort = mock(AgentJobExecutionPort.class);
		resultRecorder = mock(AgentJobExecutionResultRecorder.class);
		worker = new AgentJobDispatchWorker(
				queueConsumer,
				jobRepository,
				eventRepository,
				executionPort,
				resultRecorder,
				mock(AgentJobExecutionSuggestionRecorder.class),
				mock(AgentJobExecutionModelCallLogRecorder.class)
		);
		ReflectionTestUtils.setField(worker, "claimTimeout", Duration.ofMinutes(30));
		ReflectionTestUtils.setField(worker, "maxRetryCount", 3);
	}

	@Test
	void acknowledgesOnlyAfterPendingJobReachesARecordedOutcome() {
		AgentJob job = pendingJob();
		AgentJobQueueDelivery delivery = delivery(job.getId(), 1);
		when(queueConsumer.claim()).thenReturn(Optional.of(delivery));
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
		when(executionPort.execute(delivery.message()))
				.thenReturn(Optional.of(AgentJobExecutionOutcome.succeeded()));

		assertThat(worker.processNextQueuedJob()).isTrue();

		verify(resultRecorder).recordSucceeded(job.getId());
		verify(queueConsumer).acknowledge(delivery);
	}

	@Test
	void leavesClaimUnackedWhenDurableStartRecordingFails() {
		AgentJob job = pendingJob();
		AgentJobQueueDelivery delivery = delivery(job.getId(), 1);
		when(queueConsumer.claim()).thenReturn(Optional.of(delivery));
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
		doThrow(new IllegalStateException("event store unavailable"))
				.when(eventRepository).save(any(AgentJobEvent.class));

		assertThatThrownBy(worker::processNextQueuedJob)
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("event store unavailable");

		verify(queueConsumer, never()).acknowledge(any());
		verify(executionPort, never()).execute(any());
	}

	@Test
	void reclaimedStaleRunningJobConsumesRetryBudgetAndExecutesAgain() {
		AgentJob job = pendingJob();
		job.markRunning();
		ReflectionTestUtils.setField(job, "startedAt", Instant.now().minus(Duration.ofMinutes(31)));
		AgentJobQueueDelivery delivery = delivery(job.getId(), 2);
		when(queueConsumer.claim()).thenReturn(Optional.of(delivery));
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
		when(executionPort.execute(delivery.message()))
				.thenReturn(Optional.of(AgentJobExecutionOutcome.succeeded()));

		assertThat(worker.processNextQueuedJob()).isTrue();

		assertThat(job.getRetryCount()).isEqualTo(1);
		assertThat(job.getStartedAt()).isAfter(Instant.now().minus(Duration.ofMinutes(1)));
		ArgumentCaptor<AgentJobEvent> eventCaptor = ArgumentCaptor.forClass(AgentJobEvent.class);
		verify(eventRepository).save(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventType())
				.isEqualTo(AgentJobDispatchWorker.RECOVERED_EVENT_TYPE);
		verify(executionPort).execute(delivery.message());
		verify(queueConsumer).acknowledge(delivery);
	}

	@Test
	void reclaimedDeliveryForFreshRunningJobRemainsUnacked() {
		AgentJob job = pendingJob();
		job.markRunning();
		AgentJobQueueDelivery delivery = delivery(job.getId(), 2);
		when(queueConsumer.claim()).thenReturn(Optional.of(delivery));
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

		assertThat(worker.processNextQueuedJob()).isTrue();

		verify(queueConsumer, never()).acknowledge(any());
		verify(executionPort, never()).execute(any());
	}

	@Test
	void staleRunningJobIsFailedAndAcknowledgedWhenRetryBudgetIsExhausted() {
		AgentJob job = pendingJob();
		job.markRunning();
		ReflectionTestUtils.setField(job, "retryCount", 2);
		ReflectionTestUtils.setField(job, "startedAt", Instant.now().minus(Duration.ofMinutes(31)));
		AgentJobQueueDelivery delivery = delivery(job.getId(), 2);
		when(queueConsumer.claim()).thenReturn(Optional.of(delivery));
		when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));

		assertThat(worker.processNextQueuedJob()).isTrue();

		verify(resultRecorder).recordFailed(
				job.getId(), AgentJobDispatchWorker.STALE_ERROR_CODE, AgentJobDispatchWorker.STALE_ERROR_MESSAGE);
		verify(executionPort, never()).execute(any());
		verify(queueConsumer).acknowledge(delivery);
	}

	@Test
	void delegatesStaleClaimDiscoveryToQueueAdapter() {
		ReflectionTestUtils.setField(worker, "reclaimBatchSize", 7);
		when(queueConsumer.recoverStale(Duration.ofMinutes(30), 7)).thenReturn(2);

		assertThat(worker.recoverStaleQueueDeliveries()).isEqualTo(2);
	}

	private AgentJob pendingJob() {
		AgentJob job = AgentJob.create(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AgentJobType.ANALYZE_RESOURCE);
		ReflectionTestUtils.setField(job, "id", UUID.randomUUID());
		return job;
	}

	private AgentJobQueueDelivery delivery(UUID jobId, long attempt) {
		AgentJobQueueMessage message = new AgentJobQueueMessage(
				jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE, Instant.now());
		return new AgentJobQueueDelivery(message, "receipt-" + attempt, "payload", attempt);
	}
}
