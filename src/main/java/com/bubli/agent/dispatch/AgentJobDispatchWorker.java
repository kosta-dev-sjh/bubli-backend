package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentJobDispatchWorker {

	static final String STARTED_EVENT_TYPE = "STARTED";
	static final String STARTED_EVENT_MESSAGE = "에이전트 작업 실행을 시작했습니다.";
	static final String RECOVERED_EVENT_TYPE = "RECOVERED";
	static final String RECOVERED_EVENT_MESSAGE = "중단된 에이전트 작업 실행을 복구했습니다.";
	static final String NO_OUTCOME_ERROR_CODE = "AGENT_EXECUTION_NO_OUTCOME";
	static final String NO_OUTCOME_ERROR_MESSAGE = "에이전트 실행 결과가 반환되지 않았습니다.";
	static final String STALE_ERROR_CODE = "AGENT_EXECUTION_STALE";
	static final String STALE_ERROR_MESSAGE = "에이전트 작업이 제한 시간 내에 완료되지 않았습니다.";

	private final AgentJobQueueConsumerPort queueConsumer;
	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;
	private final AgentJobExecutionPort executionPort;
	private final AgentJobExecutionResultRecorder executionResultRecorder;
	private final AgentJobExecutionSuggestionRecorder suggestionRecorder;
	private final AgentJobExecutionModelCallLogRecorder modelCallLogRecorder;

	@Value("${agent.dispatch.worker.claim-timeout:30m}")
	private Duration claimTimeout = Duration.ofMinutes(30);

	@Value("${agent.dispatch.worker.reclaim-batch-size:20}")
	private int reclaimBatchSize = 20;

	@Value("${agent.dispatch.retry.max-retry-count:3}")
	private int maxRetryCount = 3;

	public boolean processNextQueuedJob() {
		return queueConsumer.claim()
				.map(this::processClaimed)
				.orElse(false);
	}

	public int recoverStaleQueueDeliveries() {
		return queueConsumer.recoverStale(claimTimeout, reclaimBatchSize);
	}

	private boolean processClaimed(AgentJobQueueDelivery delivery) {
		DeliveryDisposition disposition = process(delivery);
		if (disposition == DeliveryDisposition.ACKNOWLEDGE) {
			queueConsumer.acknowledge(delivery);
		}
		return true;
	}

	private DeliveryDisposition process(AgentJobQueueDelivery delivery) {
		AgentJobQueueMessage message = delivery.message();
		log.info(
				"Claimed agent job queue message. jobId={}, jobType={}, deliveryAttempt={}, roomId={}, resourceId={}, enqueuedAt={}",
				message.jobId(), message.jobType(), delivery.deliveryAttempt(), message.roomId(),
				message.resourceId(), message.enqueuedAt()
		);
		return agentJobRepository.findById(message.jobId())
				.map(agentJob -> processExisting(agentJob, delivery))
				.orElse(DeliveryDisposition.ACKNOWLEDGE);
	}

	private DeliveryDisposition processExisting(AgentJob agentJob, AgentJobQueueDelivery delivery) {
		if (agentJob.getStatus() == AgentJobStatus.PENDING) {
			markStarted(agentJob);
			execute(agentJob, delivery.message());
			return DeliveryDisposition.ACKNOWLEDGE;
		}
		if (agentJob.getStatus() == AgentJobStatus.RUNNING) {
			return processRunning(agentJob, delivery);
		}
		log.info("Acknowledging duplicate delivery for terminal/non-runnable job. jobId={}, status={}",
				agentJob.getId(), agentJob.getStatus());
		return DeliveryDisposition.ACKNOWLEDGE;
	}

	private DeliveryDisposition processRunning(AgentJob agentJob, AgentJobQueueDelivery delivery) {
		if (delivery.deliveryAttempt() <= 1) {
			log.info("Acknowledging duplicate first delivery for RUNNING job. jobId={}", agentJob.getId());
			return DeliveryDisposition.ACKNOWLEDGE;
		}
		if (!isStale(agentJob)) {
			log.info("Retaining reclaimed delivery while job is still within its execution lease. jobId={}",
					agentJob.getId());
			return DeliveryDisposition.RETAIN;
		}
		if (agentJob.getRetryCount() + 1 >= maxRetryCount) {
			executionResultRecorder.recordFailed(agentJob.getId(), STALE_ERROR_CODE, STALE_ERROR_MESSAGE);
			return DeliveryDisposition.ACKNOWLEDGE;
		}
		agentJob.restartStaleExecution();
		agentJobRepository.save(agentJob);
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(), RECOVERED_EVENT_TYPE, RECOVERED_EVENT_MESSAGE));
		execute(agentJob, delivery.message());
		return DeliveryDisposition.ACKNOWLEDGE;
	}

	private boolean isStale(AgentJob agentJob) {
		Instant startedAt = agentJob.getStartedAt();
		return startedAt == null || !startedAt.isAfter(Instant.now().minus(claimTimeout));
	}

	private void execute(AgentJob agentJob, AgentJobQueueMessage message) {
		log.info("Starting agent job execution. jobId={}, jobType={}, retryCount={}",
				agentJob.getId(), agentJob.getJobType(), agentJob.getRetryCount());
		try {
			executionPort.execute(message)
					.ifPresentOrElse(
							outcome -> recordOutcome(agentJob, outcome),
							() -> executionResultRecorder.recordFailed(
									agentJob.getId(), NO_OUTCOME_ERROR_CODE, NO_OUTCOME_ERROR_MESSAGE)
					);
		} catch (RuntimeException exception) {
			log.warn("Agent job execution port threw an exception. jobId={}", agentJob.getId(), exception);
			executionResultRecorder.recordFailed(
					agentJob.getId(), "AGENT_EXECUTION_FAILED", errorMessage(exception));
		}
	}

	private void recordOutcome(AgentJob agentJob, AgentJobExecutionOutcome outcome) {
		log.info(
				"Agent job execution outcome received. jobId={}, jobType={}, successful={}, suggestionCount={}, modelCallLogCount={}, errorCode={}, errorMessage={}",
				agentJob.getId(),
				agentJob.getJobType(),
				outcome.successful(),
				outcome.suggestionDrafts().size(),
				outcome.modelCallLogs().size(),
				outcome.errorCode(),
				truncate(outcome.errorMessage())
		);
		recordModelCallLogs(agentJob, outcome);
		if (outcome.successful()) {
			if (recordSuggestions(agentJob, outcome)) {
				executionResultRecorder.recordSucceeded(agentJob.getId());
			}
			return;
		}
		executionResultRecorder.recordFailed(agentJob.getId(), outcome.errorCode(), outcome.errorMessage());
	}

	private void recordModelCallLogs(AgentJob agentJob, AgentJobExecutionOutcome outcome) {
		if (outcome.modelCallLogs().isEmpty()) {
			return;
		}
		try {
			modelCallLogRecorder.recordModelCallLogs(agentJob.getId(), outcome.modelCallLogs());
		} catch (RuntimeException exception) {
			log.warn("Failed to record agent model call logs. jobId={}", agentJob.getId(), exception);
		}
	}

	private boolean recordSuggestions(AgentJob agentJob, AgentJobExecutionOutcome outcome) {
		if (outcome.suggestionDrafts().isEmpty()) {
			return true;
		}
		try {
			suggestionRecorder.recordSuggestions(agentJob, outcome.suggestionDrafts());
			return true;
		} catch (RuntimeException exception) {
			executionResultRecorder.recordFailed(
					agentJob.getId(), "AGENT_SUGGESTION_RECORD_FAILED", errorMessage(exception));
			return false;
		}
	}

	private String errorMessage(RuntimeException exception) {
		return StringUtils.hasText(exception.getMessage())
				? exception.getMessage()
				: exception.getClass().getSimpleName();
	}

	private String truncate(String value) {
		if (value == null || value.length() <= 300) {
			return value;
		}
		return value.substring(0, 300) + "...";
	}

	private void markStarted(AgentJob agentJob) {
		agentJob.markRunning();
		agentJobRepository.save(agentJob);
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(), STARTED_EVENT_TYPE, STARTED_EVENT_MESSAGE));
	}

	private enum DeliveryDisposition {
		ACKNOWLEDGE,
		RETAIN
	}
}
