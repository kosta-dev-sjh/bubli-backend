package com.bubli.user.dto;

import java.util.UUID;

public record UserSearchResponse(
		UUID id,
		String bubliId,
		String name,
		String avatarUrl
) {
}
