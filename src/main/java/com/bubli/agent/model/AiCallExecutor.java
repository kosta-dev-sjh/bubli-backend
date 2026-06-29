package com.bubli.agent.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@Profile("ai")
public class AiCallExecutor {

    private final int maxAttempts;
    private final Duration initialDelay;

    public AiCallExecutor(
        //  최대/최소 진행횟수
            @Value("${bubli.ai.retry.max-attempts:3}") int maxAttempts,
            @Value("${bubli.ai.retry.initial-delay:1s}") Duration initialDelay
    ) {
        //최소 한번이라도 하게끔
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("AI retry max-attempts는 1 이상이어야 합니다.");
        }
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
    }
    //
    public <T> T execute(String operationName, Supplier<T> operation) {
        RuntimeException lastException = null;
        //시도횟수 증가
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
    //Duration은 시간 간격 //재시도 간격을 늘림 지수적으로
    private Duration backoffFor(int failedAttempt) {
        long multiplier = 1L << Math.min(failedAttempt - 1, 10);
        return initialDelay.multipliedBy(multiplier);
    }
    //바로 재실행보다는 살짝 텀을 주면서 재시도 성공률을 늘리기 위해
    private void sleep(Duration delay, String operationName) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiCallFailedException(operationName, 0, exception);
        }
    }
}
