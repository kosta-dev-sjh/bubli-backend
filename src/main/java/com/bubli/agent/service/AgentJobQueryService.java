package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobQueryService {

    private final AgentJobRepository agentJobRepository;
    private final ResourceSummaryRepository resourceSummaryRepository;
    private final AiDocumentRepository aiDocumentRepository;

    @Transactional(readOnly = true)
    public AgentJobResponse getJob(UUID jobId) {
        AgentJob job = agentJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_001));

        UUID resourceSummaryId = job.getResourceId() == null
                ? null
                : resourceSummaryRepository.findTopByResourceIdOrderByCreatedAtDesc(job.getResourceId())
                .map(com.bubli.resource.entity.ResourceSummary::getId)
                .orElse(null);

        UUID aiDocumentId = job.getResourceId() == null
                ? null
                : aiDocumentRepository.findByResourceId(job.getResourceId())
                .map(com.bubli.resource.entity.AiDocument::getId)
                .orElse(null);

        return AgentJobResponse.of(job, List.of(), resourceSummaryId, aiDocumentId);
    }
}
