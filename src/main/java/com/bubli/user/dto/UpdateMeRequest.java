package com.bubli.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
		@Size(max = 100) String name,
		@Size(max = 500) String avatarUrl,
		@Size(max = 20) String locale,
		@Size(max = 50) String timezone
) {
	public UpdateUserProfileCommand toCommand() {
		return new UpdateUserProfileCommand(
				trimToNull(name),
				trimToNull(avatarUrl),
				trimToNull(locale),
				trimToNull(timezone)
		);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
