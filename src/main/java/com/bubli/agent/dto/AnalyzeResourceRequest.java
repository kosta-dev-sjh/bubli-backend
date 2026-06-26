package com.bubli.agent.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnalyzeResourceRequest(
		@NotNull(message = "분석할 자료 ID는 필수입니다.")
		UUID resourceId
) {
}
