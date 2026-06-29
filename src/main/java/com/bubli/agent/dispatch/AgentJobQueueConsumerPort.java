package com.bubli.agent.dispatch;

import java.util.Optional;

public interface AgentJobQueueConsumerPort {

	Optional<AgentJobQueueMessage> poll();
}
