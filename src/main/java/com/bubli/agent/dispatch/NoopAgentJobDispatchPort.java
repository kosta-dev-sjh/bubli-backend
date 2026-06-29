package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "agent.dispatch.adapter", havingValue = "noop", matchIfMissing = true)
public class NoopAgentJobDispatchPort implements AgentJobDispatchPort {

	@Override
	public void dispatch(AgentJobDispatchCommand command) {
		// Queue adapter will replace this boundary in a later PR.
	}
}
