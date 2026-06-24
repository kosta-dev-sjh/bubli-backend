package com.bubli.agent.service;

import com.bubli.agent.dto.AgentJobEventResult;
import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.dto.CreateAgentJobCommand;
import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.repository.AgentJobEventRepository;
import com.bubli.agent.repository.AgentJobRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentJobService {

	private final AgentJobRepository agentJobRepository;
	private final AgentJobEventRepository agentJobEventRepository;

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

	@Transactional(readOnly = true)
	public PageResponse<AgentJobEventResult> getRequestedJobEvents(UUID requestedByUserId, UUID jobId, Pageable pageable) {
		getRequestedJob(requestedByUserId, jobId);
		Page<AgentJobEventResult> page = agentJobEventRepository
				.findByJobId(jobId, withEventDefaultSort(pageable))
				.map(AgentJobEventResult::from);
		return toEventPageResponse(page);
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

	private PageResponse<AgentJobEventResult> toEventPageResponse(Page<AgentJobEventResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private Pageable withEventDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("createdAt").ascending().and(Sort.by("id").ascending())
		);
	}
}
