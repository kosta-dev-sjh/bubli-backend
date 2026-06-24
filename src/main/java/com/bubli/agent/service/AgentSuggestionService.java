package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResult;
import com.bubli.agent.dto.CreateAgentSuggestionCommand;
import com.bubli.agent.dto.UpdateAgentSuggestionCommand;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.RoomAccessService;
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
public class AgentSuggestionService {

	private final AgentSuggestionRepository agentSuggestionRepository;
	private final RoomAccessService roomAccessService;

	@Transactional(readOnly = true)
	public PageResponse<AgentSuggestionResult> getPersonalSuggestions(
			UUID userId,
			AgentSuggestionStatus status,
			Pageable pageable
	) {
		Page<AgentSuggestionResult> page = agentSuggestionRepository
				.findByUserIdAndStatus(userId, status, withDefaultSort(pageable))
				.map(AgentSuggestionResult::from);
		return toPageResponse(page);
	}

	@Transactional(readOnly = true)
	public PageResponse<AgentSuggestionResult> getRoomSuggestions(
			UUID userId,
			UUID roomId,
			AgentSuggestionStatus status,
			Pageable pageable
	) {
		roomAccessService.validateActiveMember(userId, roomId);
		Page<AgentSuggestionResult> page = agentSuggestionRepository
				.findByRoomIdAndStatus(roomId, status, withDefaultSort(pageable))
				.map(AgentSuggestionResult::from);
		return toPageResponse(page);
	}

	@Transactional
	public AgentSuggestionResult createDraft(CreateAgentSuggestionCommand command) {
		AgentSuggestion suggestion = AgentSuggestion.createDraft(
				command.userId(),
				command.roomId(),
				command.jobId(),
				command.resourceId(),
				command.suggestionType(),
				command.payloadJson(),
				command.evidenceJson()
		);
		return AgentSuggestionResult.from(agentSuggestionRepository.save(suggestion));
	}

	@Transactional
	public AgentSuggestionResult updateSuggestion(UUID userId, UUID suggestionId, UpdateAgentSuggestionCommand command) {
		validateUpdateCommand(command);
		AgentSuggestion suggestion = agentSuggestionRepository.findById(suggestionId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_002));
		validateSuggestionAccess(userId, suggestion);
		suggestion.update(command.status(), command.payloadJson(), command.evidenceJson());
		return AgentSuggestionResult.from(suggestion);
	}

	private void validateUpdateCommand(UpdateAgentSuggestionCommand command) {
		if (command.status() == null && command.payloadJson() == null && command.evidenceJson() == null) {
			throw new BusinessException(ErrorCode.AGENT_400_001);
		}
		if (command.payloadJson() != null && command.payloadJson().isBlank()) {
			throw new BusinessException(ErrorCode.AGENT_400_001);
		}
		if (command.evidenceJson() != null && command.evidenceJson().isBlank()) {
			throw new BusinessException(ErrorCode.AGENT_400_001);
		}
	}

	private void validateSuggestionAccess(UUID userId, AgentSuggestion suggestion) {
		if (suggestion.getRoomId() != null) {
			roomAccessService.validateActiveMember(userId, suggestion.getRoomId());
			return;
		}
		if (!suggestion.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.AGENT_404_002);
		}
	}

	private PageResponse<AgentSuggestionResult> toPageResponse(Page<AgentSuggestionResult> page) {
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private Pageable withDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("createdAt").descending().and(Sort.by("id").descending())
		);
	}
}
