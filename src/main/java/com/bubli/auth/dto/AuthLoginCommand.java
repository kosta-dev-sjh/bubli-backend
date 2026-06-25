package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;

public record AuthLoginCommand(
		String googleIdToken,
		ClientType clientType
) {
	public static AuthLoginCommand from(AuthLoginRequest request) {
		return new AuthLoginCommand(request.googleIdToken(), request.clientType());
	}
}
