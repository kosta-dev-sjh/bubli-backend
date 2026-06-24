package com.bubli.agent.dispatch;

public interface AgentJobDispatchPort {

	void dispatch(AgentJobDispatchCommand command);
}
