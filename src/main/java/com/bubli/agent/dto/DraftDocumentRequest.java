package com.bubli.agent.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record DraftDocumentRequest(
		@NotNull UUID roomId,
		String documentType,
		List<UUID> sourceResourceIds,
		String instruction
) {
}
