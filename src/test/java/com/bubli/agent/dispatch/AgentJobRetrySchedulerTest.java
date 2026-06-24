package com.bubli.agent.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AgentJobRetrySchedulerTest {

	@Test
	void dispatchRetryableFailedJobsDelegatesToRetryDispatcherWithConfiguredLimits() {
		AgentJobRetryDispatcher retryDispatcher = mock(AgentJobRetryDispatcher.class);
		AgentJobRetryScheduler scheduler = new AgentJobRetryScheduler(retryDispatcher);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 5);
		ReflectionTestUtils.setField(scheduler, "batchSize", 10);

		scheduler.dispatchRetryableFailedJobs();

		verify(retryDispatcher).dispatchRetryableFailedJobs(5, 10);
	}

	@Test
	void dispatchRetryableFailedJobsDoesNotPropagateDispatcherFailure() {
		AgentJobRetryDispatcher retryDispatcher = mock(AgentJobRetryDispatcher.class);
		AgentJobRetryScheduler scheduler = new AgentJobRetryScheduler(retryDispatcher);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 3);
		ReflectionTestUtils.setField(scheduler, "batchSize", 20);
		doThrow(new IllegalStateException("retry failed"))
				.when(retryDispatcher)
				.dispatchRetryableFailedJobs(3, 20);

		scheduler.dispatchRetryableFailedJobs();

		verify(retryDispatcher).dispatchRetryableFailedJobs(3, 20);
	}
}
