package com.bubli.user.dto;

public record UpdateUserProfileCommand(
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
}
