package com.bubli.agent.type;

public enum AgentDispatchOutboxStatus {
	PENDING,
	DISPATCHED,
	FAILED,
	DEAD_LETTER
}
