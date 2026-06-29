package com.bubli.agent.model;

public class AiCallFailedException extends RuntimeException {

    public AiCallFailedException(String operationName, int attempts, Throwable cause) {
        super(
                "AI 작업 '%s'이(가) %d회 시도 후 실패했습니다.".formatted(operationName, attempts),
                cause
        );
    }
}
