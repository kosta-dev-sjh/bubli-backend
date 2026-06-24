package com.bubli.agent.dispatch;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AgentJobDispatchEventListener {

	private final AgentJobDispatchPort agentJobDispatchPort;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onAgentJobCreated(AgentJobDispatchEvent event) {
		agentJobDispatchPort.dispatch(event.command());
	}
}
