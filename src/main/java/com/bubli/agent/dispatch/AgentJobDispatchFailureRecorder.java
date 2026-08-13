package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentJobDispatchFailureRecorder {

	static final String ENQUEUE_FAILURE_ERROR_CODE = "AGENT_DISPATCH_ENQUEUE_FAILED";
	static final String FAILED_EVENT_TYPE = "FAILED";

	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordEnqueueFailure(AgentJobDispatchCommand command, RuntimeException exception) {
		recordFailure(command, ENQUEUE_FAILURE_ERROR_CODE, errorMessage(exception));
	}

	@Transactional
	public void recordDeadLetterFailure(AgentJobDispatchCommand command, String errorCode, String errorMessage) {
		recordFailure(command, errorCode, errorMessage);
	}

	private void recordFailure(AgentJobDispatchCommand command, String errorCode, String errorMessage) {
		agentJobRepository.findById(command.jobId())
				.ifPresent(agentJob -> {
					if (agentJob.getStatus() != AgentJobStatus.PENDING) {
						return;
					}
					agentJob.markDispatchFailed(errorCode, errorMessage);
					agentJobEventRepository.save(AgentJobEvent.create(
							command.jobId(), FAILED_EVENT_TYPE, errorMessage));
				});
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
