package com.bubli.user.dto;

import java.util.UUID;

public record MeResponse(
		UUID id,
		String bubliId,
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
	public static MeResponse from(UserResult result) {
		return new MeResponse(
				result.id(),
				result.bubliId(),
				result.name(),
				result.avatarUrl(),
				result.locale(),
				result.timezone()
		);
	}
}
