package com.bubli.agent.service;

import com.bubli.agent.dto.AiDocumentResult;
import com.bubli.agent.dto.CreateAiDocumentCommand;
import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.repository.AiDocumentRepository;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.ProjectMembershipPublicService;
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
public class AiDocumentService {

	private final AiDocumentRepository aiDocumentRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional
	public AiDocumentResult create(CreateAiDocumentCommand command) {
		AiDocument aiDocument = AiDocument.create(
				command.resourceId(),
				command.roomId(),
				command.documentType(),
				command.detectedConfidence()
		);
		return AiDocumentResult.from(aiDocumentRepository.save(aiDocument));
	}

	@Transactional(readOnly = true)
	public AiDocumentResult getByResourceId(UUID resourceId) {
		return aiDocumentRepository.findByResourceId(resourceId)
				.map(AiDocumentResult::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_003));
	}

	@Transactional(readOnly = true)
	public PageResponse<AiDocumentResult> getRoomAiDocuments(
			UUID userId,
			UUID roomId,
			AiDocumentStatus status,
			Pageable pageable
	) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		Page<AiDocumentResult> page = findRoomAiDocuments(roomId, status, withDefaultSort(pageable))
				.map(AiDocumentResult::from);
		return new PageResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}

	private Page<AiDocument> findRoomAiDocuments(UUID roomId, AiDocumentStatus status, Pageable pageable) {
		if (status == null) {
			return aiDocumentRepository.findByRoomId(roomId, pageable);
		}
		return aiDocumentRepository.findByRoomIdAndStatus(roomId, status, pageable);
	}

	private Pageable withDefaultSort(Pageable pageable) {
		if (pageable.getSort().isSorted()) {
			return pageable;
		}
		return PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				Sort.by("updatedAt").descending().and(Sort.by("id").descending())
		);
	}
}
