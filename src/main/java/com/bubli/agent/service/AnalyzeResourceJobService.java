package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeResourceJobService {

    private final AgentJobRepository agentJobRepository;
    private final ResourceAnalysisPublicService resourceAnalysisService;

    @Transactional
    public AgentJob process(UUID jobId) {
        AgentJob job = agentJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Agent job not found."));

        if (job.getJobType() != AgentJobType.ANALYZE_RESOURCE) {
            throw new IllegalArgumentException("Only ANALYZE_RESOURCE jobs can be processed.");
        }
        if (job.getStatus() != AgentJobStatus.PENDING) {
            return job;
        }

        try {
            job.start();
            resourceAnalysisService.analyzeResourceForJob(job.getResourceId(), job.getId());
            job.succeed();
            return job;
        } catch (RuntimeException e) {
            job.fail("RESOURCE_ANALYSIS_FAILED", safeMessage(e));
            return job;
        }
    }

    @Transactional
    public int processPendingBatch() {
        List<AgentJob> jobs = agentJobRepository.findTop20ByJobTypeAndStatusOrderByCreatedAtAsc(
                AgentJobType.ANALYZE_RESOURCE,
                AgentJobStatus.PENDING
        );
        jobs.forEach(job -> process(job.getId()));
        return jobs.size();
    }

    private String safeMessage(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
