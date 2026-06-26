package com.bubli.resource.dto;

import java.util.UUID;

public record ResourceAnalysisArtifacts(
        UUID resourceSummaryId,
        UUID aiDocumentId
) {
}
