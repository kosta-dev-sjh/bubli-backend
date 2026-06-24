package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobType;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobCommandService {

	private final ResourceService resourceService;
	private final AgentJobService agentJobService;

	@Transactional
	public AgentJobResult createAnalyzeResourceJob(UUID userId, UUID resourceId) {
		ResourceResult resource = resourceService.getResource(userId, resourceId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				resource.roomId(),
				resource.id(),
				AgentJobType.ANALYZE_RESOURCE
		));
	}
}
