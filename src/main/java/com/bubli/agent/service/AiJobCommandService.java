package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobType;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiJobCommandService {

	private final ResourcePublicService resourcePublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final AgentJobService agentJobService;

	@Transactional
	public AgentJobResult createAnalyzeResourceJob(UUID userId, UUID resourceId) {
		ResourceResult resource = resourcePublicService.getReadableResource(userId, resourceId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				resource.roomId(),
				resource.id(),
				AgentJobType.ANALYZE_RESOURCE
		));
	}

	@Transactional
	public AgentJobResult createGenerateRequirementsJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_REQUIREMENTS
		));
	}

	@Transactional
	public AgentJobResult createGenerateTasksJob(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_TASKS
		));
	}
}
