package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.repository.AgentDispatchOutboxRepository;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import com.bubli.agent.type.AgentJobType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
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
	void publishPendingDispatchesCommandAndMarksOutboxDispatched() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository,
				dispatchPort,
				objectMapper
		);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, payload(objectMapper, jobId));
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.PENDING), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(outbox)));

		int publishedCount = publisher.publishPending(10);

		assertThat(publishedCount).isEqualTo(1);
		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.DISPATCHED);
		assertThat(outbox.getDispatchedAt()).isNotNull();
		ArgumentCaptor<AgentJobDispatchCommand> commandCaptor = ArgumentCaptor.forClass(AgentJobDispatchCommand.class);
		verify(dispatchPort).dispatch(commandCaptor.capture());
		assertThat(commandCaptor.getValue().jobId()).isEqualTo(jobId);
		assertThat(commandCaptor.getValue().jobType()).isEqualTo(AgentJobType.ANALYZE_RESOURCE);
	}

	@Test
	void retryFailedMarksOutboxFailedAgainWhenDispatchFails() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository,
				dispatchPort,
				objectMapper
		);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, payload(objectMapper, jobId));
		outbox.markFailed("FIRST_FAILURE", "first failure");
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.FAILED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(outbox)));
		doThrow(new IllegalStateException("queue unavailable")).when(dispatchPort).dispatch(any());

		int publishedCount = publisher.retryFailed(10, 3);

		assertThat(publishedCount).isZero();
		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.FAILED);
		assertThat(outbox.getRetryCount()).isEqualTo(2);
		assertThat(outbox.getErrorCode()).isEqualTo(AgentDispatchOutboxPublisher.RETRY_FAILURE_ERROR_CODE);
		assertThat(outbox.getErrorMessage()).isEqualTo("queue unavailable");
	}

	@Test
	void retryFailedMovesMaxedOutboxToDeadLetter() throws JsonProcessingException {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository,
				dispatchPort,
				objectMapper
		);
		UUID jobId = UUID.randomUUID();
		AgentDispatchOutbox outbox = AgentDispatchOutbox.pending(jobId, payload(objectMapper, jobId));
		outbox.markFailed("FIRST_FAILURE", "first failure");
		outbox.markFailed("SECOND_FAILURE", "second failure");
		when(repository.findByStatus(eq(AgentDispatchOutboxStatus.FAILED), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(outbox)));

		int publishedCount = publisher.retryFailed(10, 2);

		assertThat(publishedCount).isZero();
		assertThat(outbox.getStatus()).isEqualTo(AgentDispatchOutboxStatus.DEAD_LETTER);
		assertThat(outbox.getErrorCode()).isEqualTo(AgentDispatchOutboxPublisher.DEAD_LETTER_ERROR_CODE);
		assertThat(outbox.getErrorMessage()).isEqualTo(AgentDispatchOutboxPublisher.DEAD_LETTER_MESSAGE);
		verify(dispatchPort, never()).dispatch(any());
	}

	@Test
	void publishPendingDoesNothingWhenBatchSizeIsNotPositive() {
		AgentDispatchOutboxRepository repository = mock(AgentDispatchOutboxRepository.class);
		AgentJobDispatchPort dispatchPort = mock(AgentJobDispatchPort.class);
		AgentDispatchOutboxPublisher publisher = new AgentDispatchOutboxPublisher(
				repository,
				dispatchPort,
				new ObjectMapper()
		);

		int publishedCount = publisher.publishPending(0);

		assertThat(publishedCount).isZero();
		verifyNoInteractions(repository, dispatchPort);
	}

	private String payload(ObjectMapper objectMapper, UUID jobId) throws JsonProcessingException {
		return objectMapper.writeValueAsString(new AgentJobDispatchCommand(
				jobId,
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				AgentJobType.ANALYZE_RESOURCE
		));
	}
}
