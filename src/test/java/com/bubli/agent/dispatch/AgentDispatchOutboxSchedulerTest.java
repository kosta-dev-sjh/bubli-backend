package com.bubli.agent.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentDispatchOutboxSchedulerTest {

	@Test
	void publishDispatchOutboxesDelegatesToPublisherWithConfiguredLimits() {
		AgentDispatchOutboxPublisher outboxPublisher = mock(AgentDispatchOutboxPublisher.class);
		AgentDispatchOutboxScheduler scheduler = new AgentDispatchOutboxScheduler(outboxPublisher);
		ReflectionTestUtils.setField(scheduler, "pendingBatchSize", 10);
		ReflectionTestUtils.setField(scheduler, "failedBatchSize", 5);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 4);

		scheduler.publishDispatchOutboxes();

		verify(outboxPublisher).publishPending(10);
		verify(outboxPublisher).retryFailed(5, 4);
	}

	@Test
	void publishDispatchOutboxesContinuesWhenPendingPublishFails() {
		AgentDispatchOutboxPublisher outboxPublisher = mock(AgentDispatchOutboxPublisher.class);
		AgentDispatchOutboxScheduler scheduler = new AgentDispatchOutboxScheduler(outboxPublisher);
		ReflectionTestUtils.setField(scheduler, "pendingBatchSize", 20);
		ReflectionTestUtils.setField(scheduler, "failedBatchSize", 10);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 3);
		doThrow(new IllegalStateException("publish failed"))
				.when(outboxPublisher)
				.publishPending(20);

		scheduler.publishDispatchOutboxes();

		verify(outboxPublisher).publishPending(20);
		verify(outboxPublisher).retryFailed(10, 3);
	}

	@Test
	void publishDispatchOutboxesDoesNotPropagateFailedRetryFailure() {
		AgentDispatchOutboxPublisher outboxPublisher = mock(AgentDispatchOutboxPublisher.class);
		AgentDispatchOutboxScheduler scheduler = new AgentDispatchOutboxScheduler(outboxPublisher);
		ReflectionTestUtils.setField(scheduler, "pendingBatchSize", 20);
		ReflectionTestUtils.setField(scheduler, "failedBatchSize", 10);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 3);
		doThrow(new IllegalStateException("retry failed"))
				.when(outboxPublisher)
				.retryFailed(10, 3);

		scheduler.publishDispatchOutboxes();

		verify(outboxPublisher).publishPending(20);
		verify(outboxPublisher).retryFailed(10, 3);
	}
}
