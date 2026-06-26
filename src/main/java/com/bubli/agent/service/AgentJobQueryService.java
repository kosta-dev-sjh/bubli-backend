package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResponse;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceAnalysisArtifacts;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobQueryService {

    private final AgentJobRepository agentJobRepository;
    private final AgentSuggestionRepository agentSuggestionRepository;
    private final ResourceAnalysisPublicService resourceAnalysisService;

    @Transactional(readOnly = true)
    public AgentJobResponse getJob(UUID jobId) {
        AgentJob job = agentJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_001));

        ResourceAnalysisArtifacts artifacts = job.getResourceId() == null
                ? new ResourceAnalysisArtifacts(null, null)
                : resourceAnalysisService.findArtifacts(job.getResourceId());

        List<UUID> suggestionIds = agentSuggestionRepository.findAllByJobIdOrderByCreatedAtAsc(job.getId())
                .stream()
                .map(com.bubli.agent.entity.AgentSuggestion::getId)
                .toList();

        return AgentJobResponse.of(job, suggestionIds, artifacts.resourceSummaryId(), artifacts.aiDocumentId());
    }
}
