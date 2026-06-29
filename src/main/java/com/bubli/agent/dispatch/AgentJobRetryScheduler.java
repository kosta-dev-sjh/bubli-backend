package com.bubli.agent.dispatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.dispatch.retry.scheduler.enabled", havingValue = "true")
public class AgentJobRetryScheduler {

	private final AgentJobRetryDispatcher retryDispatcher;

	@Value("${agent.dispatch.retry.max-retry-count:3}")
	private int maxRetryCount;

	@Value("${agent.dispatch.retry.batch-size:20}")
	private int batchSize;

	@Scheduled(fixedDelayString = "${agent.dispatch.retry.scheduler.fixed-delay-ms:60000}")
	public void dispatchRetryableFailedJobs() {
		try {
			int dispatchedCount = retryDispatcher.dispatchRetryableFailedJobs(maxRetryCount, batchSize);
			if (dispatchedCount > 0) {
				log.info("Dispatched retryable failed agent jobs. count={}", dispatchedCount);
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to dispatch retryable failed agent jobs.", exception);
		}
	}
}
