package com.bubli.agent.dto;

import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AiDocumentResponse(
		UUID id,
		UUID resourceId,
		UUID roomId,
		AiDocumentType documentType,
		BigDecimal detectedConfidence,
		AiDocumentStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static AiDocumentResponse from(AiDocumentResult result) {
		return new AiDocumentResponse(
				result.id(),
				result.resourceId(),
				result.roomId(),
				result.documentType(),
				result.detectedConfidence(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
