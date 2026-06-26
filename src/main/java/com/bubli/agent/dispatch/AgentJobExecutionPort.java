package com.bubli.agent.dispatch;

import java.util.Optional;

public interface AgentJobExecutionPort {

	Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message);
}
