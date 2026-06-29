package com.bubli.resource.dto;

import java.util.UUID;

public record ResourceAnalysisTarget(
        UUID resourceId,
        UUID roomId
) {
}
