package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(
		@NotBlank(message = "refresh token은 필수입니다.")
		String refreshToken,

		@NotNull(message = "클라이언트 종류는 필수입니다.")
		ClientType clientType
) {
	public RefreshTokenCommand toCommand() {
		return new RefreshTokenCommand(refreshToken, clientType);
	}
}
