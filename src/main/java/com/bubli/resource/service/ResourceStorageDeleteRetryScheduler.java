package com.bubli.resource.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "resource.storage-delete.retry.scheduler.enabled", havingValue = "true")
public class ResourceStorageDeleteRetryScheduler {

	private final ResourceStorageDeleteRetryWorker retryWorker;

	@Value("${resource.storage-delete.retry.batch-size:20}")
	private int batchSize;

	@Value("${resource.storage-delete.retry.max-retry-count:3}")
	private int maxRetryCount;

	@Scheduled(fixedDelayString = "${resource.storage-delete.retry.scheduler.fixed-delay-ms:60000}")
	public void retryFailedStorageDeletes() {
		try {
			int deletedCount = retryWorker.retryDeleteRequests(batchSize, maxRetryCount);
			if (deletedCount > 0) {
				log.info("Retried resource storage delete requests. deletedCount={}", deletedCount);
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to retry resource storage delete requests.", exception);
		}
	}
}
