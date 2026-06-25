package com.bubli.agent.entity;

import com.bubli.agent.type.AgentRequestStatus;
import com.bubli.agent.type.AgentRequestType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRequestTest {

    @Test
    void queuesRetryUntilMaxRetriesThenFails() {
        AgentRequest request = AgentRequest.queue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AgentRequestType.CONTRACT_CHECK,
                Map.of("documentIds", "sample"),
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                1
        );

        request.start();
        boolean queuedForRetry = request.failOrQueueRetry("temporary error");

        assertThat(queuedForRetry).isTrue();
        assertThat(request.getStatus()).isEqualTo(AgentRequestStatus.QUEUED);
        assertThat(request.getRetryCount()).isEqualTo(1);

        request.start();
        boolean queuedAgain = request.failOrQueueRetry("final error");

        assertThat(queuedAgain).isFalse();
        assertThat(request.getStatus()).isEqualTo(AgentRequestStatus.FAILED);
        assertThat(request.getRetryCount()).isEqualTo(2);
        assertThat(request.getCompletedAt()).isNotNull();
    }
}
