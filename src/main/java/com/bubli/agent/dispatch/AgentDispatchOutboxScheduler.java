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
@ConditionalOnProperty(name = "agent.dispatch.outbox.scheduler.enabled", havingValue = "true")
public class AgentDispatchOutboxScheduler {

	private final AgentDispatchOutboxPublisher outboxPublisher;

	@Value("${agent.dispatch.outbox.pending-batch-size:20}")
	private int pendingBatchSize;

	@Value("${agent.dispatch.outbox.failed-batch-size:20}")
	private int failedBatchSize;

	@Value("${agent.dispatch.outbox.max-retry-count:3}")
	private int maxRetryCount;

	@Scheduled(fixedDelayString = "${agent.dispatch.outbox.scheduler.fixed-delay-ms:60000}")
	public void publishDispatchOutboxes() {
		publishPendingOutboxes();
		retryFailedOutboxes();
	}

	private void publishPendingOutboxes() {
		try {
			int publishedCount = outboxPublisher.publishPending(pendingBatchSize);
			if (publishedCount > 0) {
				log.info("Published pending agent dispatch outboxes. count={}", publishedCount);
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to publish pending agent dispatch outboxes.", exception);
		}
	}

	private void retryFailedOutboxes() {
		try {
			int retriedCount = outboxPublisher.retryFailed(failedBatchSize, maxRetryCount);
			if (retriedCount > 0) {
				log.info("Retried failed agent dispatch outboxes. count={}", retriedCount);
			}
		} catch (RuntimeException exception) {
			log.warn("Failed to retry failed agent dispatch outboxes.", exception);
		}
	}
}
