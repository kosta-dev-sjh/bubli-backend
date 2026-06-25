package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
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

	@Transactional
	public boolean processNextQueuedJob() {
		return queueConsumer.poll()
				.map(this::process)
				.orElse(false);
	}

	private boolean process(AgentJobQueueMessage message) {
		return agentJobRepository.findById(message.jobId())
				.filter(agentJob -> agentJob.getStatus() == AgentJobStatus.PENDING)
				.map(agentJob -> markStartedAndExecute(agentJob, message))
				.orElse(false);
	}

	private boolean markStartedAndExecute(AgentJob agentJob, AgentJobQueueMessage message) {
		markStarted(agentJob);
		executionPort.execute(message)
				.ifPresent(outcome -> recordOutcome(agentJob, outcome));
		return true;
	}

	private void recordOutcome(AgentJob agentJob, AgentJobExecutionOutcome outcome) {
		if (outcome.successful()) {
			if (!recordSuggestions(agentJob, outcome)) {
				return;
			}
			executionResultRecorder.recordSucceeded(agentJob.getId());
			return;
		}
		executionResultRecorder.recordFailed(agentJob.getId(), outcome.errorCode(), outcome.errorMessage());
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
		agentJobEventRepository.save(AgentJobEvent.create(
				agentJob.getId(),
				STARTED_EVENT_TYPE,
				STARTED_EVENT_MESSAGE
		));
	}
}
