package com.bubli.auth.dto;

import com.bubli.auth.type.ClientType;

public record AuthLoginCommand(
		String googleIdToken,
		ClientType clientType
) {
}
