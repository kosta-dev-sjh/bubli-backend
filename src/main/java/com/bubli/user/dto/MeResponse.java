package com.bubli.user.dto;

import java.util.UUID;

public record MeResponse(
		UUID id,
		String email,
		String bubliId,
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
	public static MeResponse from(UserResult result) {
		return new MeResponse(
				result.id(),
				result.email(),
				result.bubliId(),
				result.name(),
				result.avatarUrl(),
				result.locale(),
				result.timezone()
		);
	}
}
