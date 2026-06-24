package com.bubli.agent.service;

import com.bubli.agent.dto.AiDocumentResult;
import com.bubli.agent.dto.CreateAiDocumentCommand;
import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.repository.AiDocumentRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiDocumentService {

	private final AiDocumentRepository aiDocumentRepository;

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
}
