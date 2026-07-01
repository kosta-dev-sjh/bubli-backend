package com.bubli.memory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomMemorySummaryRequest(
		@NotNull(message = "시작 시퀀스는 필수입니다.")
		Long fromSequence,

		@NotNull(message = "종료 시퀀스는 필수입니다.")
		Long toSequence,

		@NotBlank(message = "요약 내용은 필수입니다.")
		String summaryJson
) {
}
