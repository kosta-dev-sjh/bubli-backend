package com.bubli.user.dto;

import com.bubli.user.entity.User;

import java.util.UUID;

public record UserResult(
		UUID id,
		String email,
		String bubliId,
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
	public static UserResult from(User user, String email) {
		return new UserResult(
				user.getId(),
				email,
				user.getBubliId(),
				user.getName(),
				user.getAvatarUrl(),
				user.getLocale(),
				user.getTimezone()
		);
	}
}
