package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleCallbackRequest(
		@NotBlank(message = "Google OAuth code는 필수입니다.")
		String code,

		@NotBlank(message = "redirect URI는 필수입니다.")
		String redirectUri,

		@NotNull(message = "클라이언트 종류는 필수입니다.")
		ClientType clientType
) {
	public GoogleCallbackCommand toCommand() {
		return new GoogleCallbackCommand(code, redirectUri, clientType);
	}
}
