package com.bubli.agent.service;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobCommandService {

    private final AgentJobRepository agentJobRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public AgentJob createAnalyzeResourceJob(UUID requestedByUserId, UUID resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_404_001));

        resource.startAnalysis();

        return agentJobRepository.save(AgentJob.pending(
                requestedByUserId,
                resource.getRoomId(),
                resource.getId(),
                AgentJobType.ANALYZE_RESOURCE
        ));
    }
}
