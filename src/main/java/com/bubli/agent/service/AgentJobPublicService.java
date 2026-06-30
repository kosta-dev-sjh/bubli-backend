package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobPublicService {

    private final AgentJobService agentJobService;

    public AgentJobTicket createAnalyzeResourceJob(UUID requestedByUserId, UUID roomId, UUID resourceId) {
        return createAnalyzeResourceJob(requestedByUserId, roomId, resourceId, null);
    }

    public AgentJobTicket createAnalyzeResourceJob(
            UUID requestedByUserId,
            UUID roomId,
            UUID resourceId,
            Map<String, Object> requestPayload
    ) {
        var result = agentJobService.create(requestedByUserId, new CreateAgentJobCommand(
                roomId,
                resourceId,
                AgentJobType.ANALYZE_RESOURCE,
                requestPayload
        ));
        return new AgentJobTicket(result.id(), result.status());
    }
}
