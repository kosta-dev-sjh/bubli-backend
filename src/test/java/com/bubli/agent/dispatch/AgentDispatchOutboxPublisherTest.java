package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.repository.AgentDispatchOutboxRepository;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
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

class AgentDispatchOutboxPublisherTest {

	@Test
	void publishPendingDispatchesAndRecordsQueued() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		AgentJobDispatchSuccessRecorder successRecorder = mock(AgentJobDispatchSuccessRecorder.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository, dispatchPort, objectMapper, failureRecorder, successRecorder);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, payload(objectMapper, jobId));
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.PENDING), any(Pageable.class)))
				.thenReturn(List.of(outbox));

		assertThat(publisher.publishPending(10)).isEqualTo(1);

		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.DISPATCHED);
		ArgumentCaptor<AgentJobDispatchCommand> commandCaptor = ArgumentCaptor.forClass(AgentJobDispatchCommand.class);
		verify(dispatchPort).dispatch(commandCaptor.capture());
		verify(successRecorder).recordQueued(commandCaptor.getValue());
		verifyNoInteractions(failureRecorder);
	}

	@Test
	void retryFailedKeepsOutboxFailedWhenQueueIsUnavailable() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository, dispatchPort, objectMapper,
				mock(AgentJobDispatchFailureRecorder.class), mock(AgentJobDispatchSuccessRecorder.class));
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(UUID.randomUUID(), payload(objectMapper, UUID.randomUUID()));
		outbox.markFailed("FIRST_FAILURE", "first failure");
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.FAILED), any(Pageable.class)))
				.thenReturn(List.of(outbox));
		doThrow(new IllegalStateException("queue unavailable")).when(dispatchPort).dispatch(any());

		assertThat(publisher.retryFailed(10, 3)).isZero();

		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.FAILED);
		assertThat(outbox.getRetryCount()).isEqualTo(2);
		assertThat(outbox.getErrorCode()).isEqualTo(AgentDispatchOutboxPublisher.RETRY_FAILURE_ERROR_CODE);
	}

	@Test
	void retryFailedDeadLettersOutboxAndFailsPendingJobAtLimit() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentJobDispatchFailureRecorder failureRecorder = mock(AgentJobDispatchFailureRecorder.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository, dispatchPort, objectMapper, failureRecorder,
				mock(AgentJobDispatchSuccessRecorder.class));
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, payload(objectMapper, jobId));
		outbox.markFailed("FIRST_FAILURE", "first failure");
		outbox.markFailed("SECOND_FAILURE", "second failure");
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.FAILED), any(Pageable.class)))
				.thenReturn(List.of(outbox));

		assertThat(publisher.retryFailed(10, 2)).isZero();

		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.DEAD_LETTER);
		verify(failureRecorder).recordDeadLetterFailure(
				any(AgentJobDispatchCommand.class),
				eq(AgentDispatchOutboxPublisher.DEAD_LETTER_ERROR_CODE),
				eq(AgentDispatchOutboxPublisher.DEAD_LETTER_MESSAGE));
		verify(dispatchPort, never()).dispatch(any());
	}

	@Test
	void publishPendingDoesNothingForNonPositiveBatch() {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository, dispatchPort, new ObjectMapper(),
				mock(AgentJobDispatchFailureRecorder.class), mock(AgentJobDispatchSuccessRecorder.class));

		assertThat(publisher.publishPending(0)).isZero();

		verifyNoInteractions(repository, dispatchPort);
	}

	private String payload(ObjectMapper objectMapper, UUID jobId) throws JsonProcessingException {
		return objectMapper.writeValueAsString(new AgentJobDispatchCommand(
				jobId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AgentJobType.ANALYZE_RESOURCE));
	}
}
