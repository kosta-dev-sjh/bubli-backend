package com.bubli.agent.dto;

import com.bubli.agent.type.AiDocumentType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAiDocumentCommand(
		UUID resourceId,
		UUID roomId,
		AiDocumentType documentType,
		BigDecimal detectedConfidence
) {
}
