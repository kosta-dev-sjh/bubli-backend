package com.bubli.personal.calendar.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleCalendarCallbackRequest(
		@NotBlank(message = "Google authorization code는 필수입니다.")
		String code,

		String redirectUri
) {
}
