package com.bubli.agent.dispatch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentJobDispatchEventListener {

	private final AgentJobDispatchPort agentJobDispatchPort;
	private final AgentJobDispatchFailureRecorder failureRecorder;
	private final AgentJobDispatchSuccessRecorder successRecorder;
	private final AgentJobDispatchOutboxRecorder outboxRecorder;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAgentJobCreated(AgentJobDispatchEvent event) {
		try {
			agentJobDispatchPort.dispatch(event.command());
		} catch (RuntimeException exception) {
			log.warn("Failed to dispatch agent job. jobId={}", event.command().jobId(), exception);
			failureRecorder.recordEnqueueFailure(event.command(), exception);
			recordOutboxFailure(event, exception);
			return;
		}
		try {
			outboxRecorder.recordDispatched(event.command().jobId());
		} catch (RuntimeException exception) {
			log.warn("Failed to mark agent dispatch outbox dispatched. jobId={}", event.command().jobId(), exception);
		}
		try {
			successRecorder.recordQueued(event.command());
		} catch (RuntimeException exception) {
			log.warn("Failed to record dispatched agent job event. jobId={}", event.command().jobId(), exception);
		}
	}

	private void recordOutboxFailure(AgentJobDispatchEvent event, RuntimeException dispatchException) {
		try {
			outboxRecorder.recordFailure(
					event.command().jobId(),
					AgentJobDispatchFailureRecorder.ENQUEUE_FAILURE_ERROR_CODE,
					errorMessage(dispatchException)
			);
		} catch (RuntimeException exception) {
			log.warn("Failed to mark agent dispatch outbox failed. jobId={}", event.command().jobId(), exception);
		}
	}

	private String errorMessage(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return message;
	}
}
