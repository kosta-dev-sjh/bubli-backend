package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentJobDispatchWorker {

	static final String STARTED_EVENT_TYPE = "STARTED";
	static final String STARTED_EVENT_MESSAGE = "에이전트 작업 실행을 시작했습니다.";

	private final AgentJobQueueConsumerPort queueConsumer;
	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;
	private final AgentJobExecutionPort executionPort;
	private final AgentJobExecutionResultRecorder executionResultRecorder;
	private final AgentJobExecutionSuggestionRecorder suggestionRecorder;
	private final AgentJobExecutionModelCallLogRecorder modelCallLogRecorder;

	public boolean processNextQueuedJob() {
		return queueConsumer.poll()
				.map(this::process)
				.orElse(false);
	}

	private boolean process(AgentJobQueueMessage message) {
		log.info(
				"Polled agent job queue message. jobId={}, jobType={}, roomId={}, resourceId={}, enqueuedAt={}",
				message.jobId(),
				message.jobType(),
				message.roomId(),
				message.resourceId(),
				message.enqueuedAt()
		);
		return agentJobRepository.findById(message.jobId())
				.filter(this::isPending)
				.map(agentJob -> markStartedAndExecute(agentJob, message))
				.orElse(false);
	}

	private boolean markStartedAndExecute(AgentJob agentJob, AgentJobQueueMessage message) {
		log.info(
				"Starting agent job execution. jobId={}, jobType={}, roomId={}, resourceId={}, retryCount={}",
				agentJob.getId(),
				agentJob.getJobType(),
				agentJob.getRoomId(),
				agentJob.getResourceId(),
				agentJob.getRetryCount()
		);
		markStarted(agentJob);
		try {
			executionPort.execute(message)
					.ifPresent(outcome -> recordOutcome(agentJob, outcome));
		} catch (RuntimeException exception) {
			log.warn("Agent job execution port threw an exception. jobId={}", agentJob.getId(), exception);
			executionResultRecorder.recordFailed(
					agentJob.getId(),
					"AGENT_EXECUTION_FAILED",
					errorMessage(exception)
			);
		}
		return true;
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
			if (!recordSuggestions(agentJob, outcome)) {
				return;
			}
			executionResultRecorder.recordSucceeded(agentJob.getId());
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
					agentJob.getId(),
					"AGENT_SUGGESTION_RECORD_FAILED",
					errorMessage(exception)
			);
			return false;
		}
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (!StringUtils.hasText(message)) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}

	private void markStarted(AgentJob agentJob) {
		agentJob.markRunning();
		agentJobRepository.save(agentJob);
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				STARTED_EVENT_TYPE,
				STARTED_EVENT_MESSAGE
		));
	}

	private boolean isPending(AgentJob agentJob) {
		if (agentJob.getStatus() == AgentJobStatus.PENDING) {
			return true;
		}
		log.warn(
				"Skipping queued agent job because status is not PENDING. jobId={}, jobType={}, status={}, retryCount={}, errorCode={}, errorMessage={}",
				agentJob.getId(),
				agentJob.getJobType(),
				agentJob.getStatus(),
				agentJob.getRetryCount(),
				agentJob.getErrorCode(),
				truncate(agentJob.getErrorMessage())
		);
		return false;
	}

	private String truncate(String value) {
		if (value == null || value.length() <= 300) {
			return value;
		}
		return value.substring(0, 300) + "...";
	}
}
