package com.bubli.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record RecordCurrentAppActivityRequest(
		UUID roomId,

		@Size(max = 120, message = "앱 이름은 120자 이하여야 합니다.")
		String appName,

		@Size(max = 500, message = "창 제목은 500자 이하여야 합니다.")
		String windowTitle,

		@NotNull(message = "활동 시작 시각은 필수입니다.")
		Instant startedAt,

		Instant endedAt,

		@PositiveOrZero(message = "활동 시간은 0 이상이어야 합니다.")
		Long durationSeconds
) {

	public RecordCurrentAppActivityCommand toCommand() {
		return new RecordCurrentAppActivityCommand(
				roomId,
				blankToNull(appName),
				blankToNull(windowTitle),
				startedAt,
				endedAt,
				durationSeconds
		);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
