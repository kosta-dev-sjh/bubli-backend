package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.dto.ResourceAnalysisTarget;
import com.bubli.resource.service.ResourceAnalysisPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobCommandService {

    private final AgentJobRepository agentJobRepository;
    private final ResourceAnalysisPublicService resourceAnalysisService;

    @Transactional
    public AgentJob createAnalyzeResourceJob(UUID requestedByUserId, UUID resourceId) {
        ResourceAnalysisTarget target = resourceAnalysisService.startAnalysis(resourceId);
        return agentJobRepository.save(AgentJob.pending(
                requestedByUserId,
                target.roomId(),
                target.resourceId(),
                AgentJobType.ANALYZE_RESOURCE
        ));
    }
}
