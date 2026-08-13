package com.bubli.agent.dispatch;

import java.util.Objects;

/**
 * A claimed queue message. The receipt and raw payload are transport-specific and
 * must be returned to the queue adapter when processing is durably complete.
 */
public record AgentJobQueueDelivery(
		AgentJobQueueMessage message,
		String receipt,
		String rawPayload,
		long deliveryAttempt
) {

	public AgentJobQueueDelivery {
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(receipt, "receipt");
		if (deliveryAttempt < 1) {
			throw new IllegalArgumentException("deliveryAttempt must be positive.");
		}
	}

	public static AgentJobQueueDelivery inMemory(AgentJobQueueMessage message) {
		return new AgentJobQueueDelivery(message, message.jobId().toString(), null, 1);
	}
}
