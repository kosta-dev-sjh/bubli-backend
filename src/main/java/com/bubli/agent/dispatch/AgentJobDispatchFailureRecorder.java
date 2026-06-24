package com.bubli.agent.dispatch;

import com.bubli.agent.entity.AgentJobEvent;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
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
		agentJobRepository.findById(command.jobId())
				.ifPresent(agentJob -> {
					String message = errorMessage(exception);
					agentJob.markDispatchFailed(ENQUEUE_FAILURE_ERROR_CODE, message);
					agentJobEventRepository.save(AgentJobEvent.create(command.jobId(), FAILED_EVENT_TYPE, message));
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
