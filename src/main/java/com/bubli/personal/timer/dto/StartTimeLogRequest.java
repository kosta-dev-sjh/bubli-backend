package com.bubli.personal.timer.dto;

import com.bubli.personal.timer.type.TimerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StartTimeLogRequest(
		UUID roomId,
		TimerType timerType,

		@NotBlank(message = "타이머 중복 방지 키는 필수입니다.")
		@Size(max = 120, message = "타이머 중복 방지 키는 120자 이하여야 합니다.")
		String idempotencyKey,

		UUID recoveredFromTimeLogId
) {
}
