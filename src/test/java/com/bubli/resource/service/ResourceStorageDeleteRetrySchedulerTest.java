package com.bubli.resource.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResourceStorageDeleteRetrySchedulerTest {

	@Test
	void retryFailedStorageDeletesDelegatesToWorkerWithConfiguredLimits() {
		ResourceStorageDeleteRetryWorker retryWorker = mock(ResourceStorageDeleteRetryWorker.class);
		ResourceStorageDeleteRetryScheduler scheduler = new ResourceStorageDeleteRetryScheduler(retryWorker);
		ReflectionTestUtils.setField(scheduler, "batchSize", 10);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 4);

		scheduler.retryFailedStorageDeletes();

		verify(retryWorker).retryDeleteRequests(10, 4);
	}

	@Test
	void retryFailedStorageDeletesDoesNotPropagateWorkerFailure() {
		ResourceStorageDeleteRetryWorker retryWorker = mock(ResourceStorageDeleteRetryWorker.class);
		ResourceStorageDeleteRetryScheduler scheduler = new ResourceStorageDeleteRetryScheduler(retryWorker);
		ReflectionTestUtils.setField(scheduler, "batchSize", 20);
		ReflectionTestUtils.setField(scheduler, "maxRetryCount", 3);
		doThrow(new IllegalStateException("retry failed"))
				.when(retryWorker)
				.retryDeleteRequests(20, 3);

		scheduler.retryFailedStorageDeletes();

		verify(retryWorker).retryDeleteRequests(20, 3);
	}
}
