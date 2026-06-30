package com.bubli.agent.service;

public class AgentModelUsageLimitExceededException extends RuntimeException {

	private final String errorCode;

	public AgentModelUsageLimitExceededException(String errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public String errorCode() {
		return errorCode;
	}
}
