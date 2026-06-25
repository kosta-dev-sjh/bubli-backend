package com.bubli.agent.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiCallExecutorTest {

    @Test
    void retriesTransientFailureAndReturnsResult() {
        AtomicInteger attempts = new AtomicInteger();
        AiCallExecutor executor = new AiCallExecutor(3, Duration.ZERO);

        String result = executor.execute("test", () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary");
            }
            return "success";
        });

        assertThat(result).isEqualTo("success");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void throwsAfterConfiguredAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        AiCallExecutor executor = new AiCallExecutor(2, Duration.ZERO);

        assertThatThrownBy(() -> executor.execute("test", () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("always fails");
        }))
                .isInstanceOf(AiCallFailedException.class)
                .hasMessageContaining("2회");

        assertThat(attempts).hasValue(2);
    }
}
