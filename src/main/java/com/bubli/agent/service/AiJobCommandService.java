package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.type.AgentJobType;
import com.bubli.project.service.RoomAccessService;
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
	private final RoomAccessService roomAccessService;
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

	@Transactional
	public AgentJobResult createGenerateRequirementsJob(UUID userId, UUID roomId) {
		roomAccessService.validateActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_REQUIREMENTS
		));
	}

	@Transactional
	public AgentJobResult createGenerateTasksJob(UUID userId, UUID roomId) {
		roomAccessService.validateActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_TASKS
		));
	}

	@Transactional
	public AgentJobResult createGenerateWbsJob(UUID userId, UUID roomId) {
		roomAccessService.validateActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_WBS
		));
	}

	@Transactional
	public AgentJobResult createGenerateQuestionsJob(UUID userId, UUID roomId) {
		roomAccessService.validateActiveMember(userId, roomId);
		return agentJobService.create(userId, new CreateAgentJobCommand(
				roomId,
				null,
				AgentJobType.GENERATE_QUESTIONS
		));
	}
}
