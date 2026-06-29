package com.bubli.agent.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentJobWorkerSchedulerTest {

	@Test
	void processQueuedJobsStopsWhenQueueIsEmpty() {
		AgentJobDispatchWorker dispatchWorker = mock(AgentJobDispatchWorker.class);
		AgentJobWorkerScheduler scheduler = new AgentJobWorkerScheduler(dispatchWorker);
		ReflectionTestUtils.setField(scheduler, "maxJobsPerTick", 5);
		when(dispatchWorker.processNextQueuedJob())
				.thenReturn(true)
				.thenReturn(true)
				.thenReturn(false);

		scheduler.processQueuedJobs();

		verify(dispatchWorker, times(3)).processNextQueuedJob();
	}

	@Test
	void processQueuedJobsStopsAtConfiguredLimit() {
		AgentJobDispatchWorker dispatchWorker = mock(AgentJobDispatchWorker.class);
		AgentJobWorkerScheduler scheduler = new AgentJobWorkerScheduler(dispatchWorker);
		ReflectionTestUtils.setField(scheduler, "maxJobsPerTick", 2);
		when(dispatchWorker.processNextQueuedJob()).thenReturn(true);

		scheduler.processQueuedJobs();

		verify(dispatchWorker, times(2)).processNextQueuedJob();
	}

	@Test
	void processQueuedJobsDoesNotPropagateWorkerFailure() {
		AgentJobDispatchWorker dispatchWorker = mock(AgentJobDispatchWorker.class);
		AgentJobWorkerScheduler scheduler = new AgentJobWorkerScheduler(dispatchWorker);
		ReflectionTestUtils.setField(scheduler, "maxJobsPerTick", 5);
		doThrow(new IllegalStateException("worker failed"))
				.when(dispatchWorker)
				.processNextQueuedJob();

		scheduler.processQueuedJobs();

		verify(dispatchWorker).processNextQueuedJob();
	}
}
