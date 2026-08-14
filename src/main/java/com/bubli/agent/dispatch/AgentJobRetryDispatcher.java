package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentJobRetryDispatcher {

	private final AgentJobRepository agentJobRepository;
	private final AgentJobDispatchOutboxRecorder outboxRecorder;
	private final ApplicationEventPublisher eventPublisher;

	/**
	 * Persists FAILED -> PENDING and the outbox reset in one transaction. The queue
	 * fast-path runs only AFTER_COMMIT, so a worker can never observe the retry
	 * message while the database still says FAILED.
	 */
	@Transactional
	public int dispatchRetryableFailedJobs(int maxRetryCount, int batchSize) {
		Pageable pageable = PageRequest.of(0, Math.max(1, batchSize));
		List<AgentJob> retryableJobs = agentJobRepository
				.findByStatusAndRetryCountLessThan(AgentJobStatus.FAILED, maxRetryCount, pageable)
				.getContent();

		int queuedCount = 0;
		for (AgentJob agentJob : retryableJobs) {
			AgentJobDispatchCommand command = AgentJobDispatchCommand.from(agentJob);
			agentJob.markRetryQueued();
			outboxRecorder.recordPending(command);
			eventPublisher.publishEvent(new AgentJobDispatchEvent(command));
			queuedCount++;
			log.info("Retryable agent job transactionally queued. jobId={}, retryCount={}",
					agentJob.getId(), agentJob.getRetryCount());
		}
		return queuedCount;
	}
}
