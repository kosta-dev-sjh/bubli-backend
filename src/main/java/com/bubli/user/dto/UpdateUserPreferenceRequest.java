package com.bubli.user.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserPreferenceRequest(
		@Size(max = 30) String theme,
		@Size(max = 30) String defaultHomeType,
		UUID defaultProjectRoomId
) {
	public UpdateUserPreferenceCommand toCommand() {
		return new UpdateUserPreferenceCommand(
				trimToNull(theme),
				trimToNull(defaultHomeType),
				defaultProjectRoomId
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
