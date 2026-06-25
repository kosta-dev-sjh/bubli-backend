package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;

public record GoogleCallbackCommand(
		String code,
		String redirectUri,
		ClientType clientType
) {
}
