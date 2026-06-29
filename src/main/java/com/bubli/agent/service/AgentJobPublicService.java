package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.agent.type.AgentJobType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobPublicService {

    private final AgentJobRepository agentJobRepository;

    @Transactional
    public AgentJobTicket createAnalyzeResourceJob(UUID requestedByUserId, UUID roomId, UUID resourceId) {
        AgentJob job = agentJobRepository.save(AgentJob.pending(
                requestedByUserId,
                roomId,
                resourceId,
                AgentJobType.ANALYZE_RESOURCE
        ));
        return AgentJobTicket.from(job);
    }
}
