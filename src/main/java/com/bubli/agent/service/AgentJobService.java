package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobService {

	private final AgentJobRepository agentJobRepository;

	@Transactional
	public AgentJobResult create(UUID requestedByUserId, CreateAgentJobCommand command) {
		AgentJob agentJob = AgentJob.create(
				requestedByUserId,
				command.roomId(),
				command.resourceId(),
				command.jobType()
		);
		return AgentJobResult.from(agentJobRepository.save(agentJob));
	}

	@Transactional(readOnly = true)
	public AgentJobResult getRequestedJob(UUID requestedByUserId, UUID jobId) {
		return agentJobRepository.findByIdAndRequestedByUserId(jobId, requestedByUserId)
				.map(AgentJobResult::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_001));
	}

	@Transactional
	public AgentJobResult markRunning(UUID jobId) {
		AgentJob agentJob = getJob(jobId);
		agentJob.markRunning();
		return AgentJobResult.from(agentJob);
	}

	@Transactional
	public AgentJobResult markSucceeded(UUID jobId) {
		AgentJob agentJob = getJob(jobId);
		agentJob.markSucceeded();
		return AgentJobResult.from(agentJob);
	}

	@Transactional
	public AgentJobResult markFailed(UUID jobId, String errorCode, String errorMessage) {
		AgentJob agentJob = getJob(jobId);
		agentJob.markFailed(errorCode, errorMessage);
		return AgentJobResult.from(agentJob);
	}

	private AgentJob getJob(UUID jobId) {
		return agentJobRepository.findById(jobId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_001));
	}
}
