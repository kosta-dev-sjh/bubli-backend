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
@ConditionalOnProperty(name = "agent.dispatch.worker.scheduler.enabled", havingValue = "true")
public class AgentJobWorkerScheduler {

	private final AgentJobDispatchWorker dispatchWorker;

	@Value("${agent.dispatch.worker.max-jobs-per-tick:5}")
	private int maxJobsPerTick;

	@Scheduled(fixedDelayString = "${agent.dispatch.worker.scheduler.fixed-delay-ms:1000}")
	public void processQueuedJobs() {
		int processedCount = 0;
		for (int index = 0; index < maxJobsPerTick; index++) {
			try {
				if (!dispatchWorker.processNextQueuedJob()) {
					break;
				}
				processedCount++;
			} catch (RuntimeException exception) {
				log.warn("Failed to process queued agent job.", exception);
				break;
			}
		}
		if (processedCount > 0) {
			log.info("Processed queued agent jobs. count={}", processedCount);
		}
	}
}
