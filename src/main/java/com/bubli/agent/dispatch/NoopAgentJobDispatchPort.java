package com.bubli.agent.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(AgentJobDispatchPort.class)
public class NoopAgentJobDispatchPort implements AgentJobDispatchPort {

	@Override
	public void dispatch(AgentJobDispatchCommand command) {
		// Queue adapter will replace this boundary in a later PR.
	}
}
