package com.bubli.agent.dispatch;

import java.time.Duration;
import java.util.Optional;

public interface AgentJobQueueConsumerPort {

	/**
	 * Atomically claims the next message. A claimed delivery remains recoverable
	 * until {@link #acknowledge(AgentJobQueueDelivery)} is called.
	 */
	Optional<AgentJobQueueDelivery> claim();

	default void acknowledge(AgentJobQueueDelivery delivery) {
		// Transports without an acknowledgement concept complete on claim.
	}

	default int recoverStale(Duration claimTimeout, int limit) {
		return 0;
	}
}
