package com.bubli.user.dto;

import com.bubli.user.entity.User;
import com.bubli.global.locale.SupportedLocale;

import java.util.UUID;

public record UserResult(
		UUID id,
		String bubliId,
		String name,
		String avatarUrl,
		String locale,
		String timezone
) {
	public static UserResult from(User user) {
		return new UserResult(
				user.getId(),
				user.getBubliId(),
				user.getName(),
				user.getAvatarUrl(),
				SupportedLocale.normalize(user.getLocale()),
				user.getTimezone()
		);
	}
}
