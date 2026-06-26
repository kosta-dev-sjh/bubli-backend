package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;

public record GoogleAuthorizeCommand(
		String redirectUri,
		ClientType clientType,
		String state
) {
}
