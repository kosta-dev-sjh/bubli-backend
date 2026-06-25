package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;

public record RefreshTokenCommand(
		String refreshToken,
		ClientType clientType
) {
	public static RefreshTokenCommand from(RefreshTokenRequest request) {
		return new RefreshTokenCommand(request.refreshToken(), request.clientType());
	}
}
