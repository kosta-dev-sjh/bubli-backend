package com.bubli.agent.dto;

import com.bubli.agent.entity.AiDocument;
import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiDocumentResult(
		UUID id,
		UUID resourceId,
		UUID roomId,
		AiDocumentType documentType,
		BigDecimal detectedConfidence,
		AiDocumentStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static AiDocumentResult from(AiDocument aiDocument) {
		return new AiDocumentResult(
				aiDocument.getId(),
				aiDocument.getResourceId(),
				aiDocument.getRoomId(),
				aiDocument.getDocumentType(),
				aiDocument.getDetectedConfidence(),
				aiDocument.getStatus(),
				aiDocument.getCreatedAt(),
				aiDocument.getUpdatedAt()
		);
	}
}
