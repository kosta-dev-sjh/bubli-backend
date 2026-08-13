package com.bubli.global.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class AiCallExecutor {

	private final int maxAttempts;
	private final Duration initialDelay;

	public AiCallExecutor(
			@Value("${bubli.ai.retry.max-attempts:3}") int maxAttempts,
			@Value("${bubli.ai.retry.initial-delay:1s}") Duration initialDelay
	) {
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("AI retry max-attempts는 1 이상이어야 합니다.");
		}
		this.maxAttempts = maxAttempts;
		this.initialDelay = initialDelay;
	}

	public <T> T execute(String operationName, Supplier<T> operation) {
		RuntimeException lastException = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return operation.get();
			} catch (RuntimeException exception) {
				lastException = exception;
				if (attempt < maxAttempts) {
					sleep(backoffFor(attempt), operationName);
				}
			}
		}
		throw new AiCallFailedException(operationName, maxAttempts, lastException);
	}

	private Duration backoffFor(int failedAttempt) {
		long multiplier = 1L << Math.min(failedAttempt - 1, 10);
		return initialDelay.multipliedBy(multiplier);
	}

	private void sleep(Duration delay, String operationName) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AiCallFailedException(operationName, 0, exception);
		}
	}
}
