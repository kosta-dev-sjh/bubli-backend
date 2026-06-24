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

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAgentJobCreated(AgentJobDispatchEvent event) {
		try {
			agentJobDispatchPort.dispatch(event.command());
		} catch (RuntimeException exception) {
			log.warn("Failed to dispatch agent job. jobId={}", event.command().jobId(), exception);
			failureRecorder.recordEnqueueFailure(event.command(), exception);
		}
	}
}
