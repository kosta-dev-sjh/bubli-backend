package com.bubli.user.dto;

public record UpsertGoogleUserCommand(
		String googleSub,
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
}
