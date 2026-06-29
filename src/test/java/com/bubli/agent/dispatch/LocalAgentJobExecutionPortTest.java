package com.bubli.agent.dispatch;

import com.bubli.agent.type.AgentJobType;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalAgentJobExecutionPortTest {

    @Test
    void analyzesResourceJob() {
        ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                resourceAnalysisService,
                new ObjectMapper()
        );
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                jobId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                resourceId,
                AgentJobType.ANALYZE_RESOURCE,
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        verify(resourceAnalysisService).analyzeResourceForJob(resourceId, jobId);
    }

    @Test
    void generatesTaskSuggestionDraft() {
        LocalAgentJobExecutionPort executionPort = new LocalAgentJobExecutionPort(
                mock(ResourceAnalysisPublicService.class),
                new ObjectMapper()
        );

        var outcome = executionPort.execute(new AgentJobQueueMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                AgentJobType.GENERATE_TASKS,
                Instant.now()
        ));

        assertThat(outcome).isPresent();
        assertThat(outcome.get().successful()).isTrue();
        assertThat(outcome.get().suggestionDrafts()).hasSize(1);
        assertThat(outcome.get().suggestionDrafts().getFirst().suggestionType()).isEqualTo(AgentSuggestionType.TASK);
        assertThat(outcome.get().modelCallLogs()).hasSize(1);
    }
}
