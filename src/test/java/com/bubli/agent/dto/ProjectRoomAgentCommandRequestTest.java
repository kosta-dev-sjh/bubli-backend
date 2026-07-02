package com.bubli.agent.dto;

import com.bubli.agent.type.AgentCommandMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectRoomAgentCommandRequestTest {

    @Test
    void defaultsMissingResourceIdsToEmptyList() {
        ProjectRoomAgentCommandRequest request = new ProjectRoomAgentCommandRequest(
                "answer this",
                AgentCommandMode.ANSWER,
                null
        );

        assertThat(request.resourceIds()).isEmpty();
    }
}
