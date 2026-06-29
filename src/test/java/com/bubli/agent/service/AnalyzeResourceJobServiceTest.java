package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzeResourceJobServiceTest {

    @Test
    void processesAnalyzeResourceJob() {
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        AgentJob job = job(jobId, resourceId);
        AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
        ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);

        when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        AgentJob result = new AnalyzeResourceJobService(agentJobRepository, resourceAnalysisService).process(jobId);

        assertThat(result.getStatus()).isEqualTo(AgentJobStatus.SUCCEEDED);
        verify(resourceAnalysisService).analyzeResourceForJob(resourceId, jobId);
    }

    @Test
    void marksJobFailedWhenResourceAnalysisFails() {
        UUID jobId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        AgentJob job = job(jobId, resourceId);
        AgentJobRepository agentJobRepository = mock(AgentJobRepository.class);
        ResourceAnalysisPublicService resourceAnalysisService = mock(ResourceAnalysisPublicService.class);

        when(agentJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        doThrow(new IllegalArgumentException("Extracted text is empty."))
                .when(resourceAnalysisService).analyzeResourceForJob(resourceId, jobId);

        AgentJob result = new AnalyzeResourceJobService(agentJobRepository, resourceAnalysisService).process(jobId);

        assertThat(result.getStatus()).isEqualTo(AgentJobStatus.FAILED);
        assertThat(result.getErrorCode()).isEqualTo("RESOURCE_ANALYSIS_FAILED");
        assertThat(result.getErrorMessage()).isEqualTo("Extracted text is empty.");
    }

    private AgentJob job(UUID jobId, UUID resourceId) {
        AgentJob job = AgentJob.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                resourceId,
                AgentJobType.ANALYZE_RESOURCE
        );
        ReflectionTestUtils.setField(job, "id", jobId);
        return job;
    }
}
