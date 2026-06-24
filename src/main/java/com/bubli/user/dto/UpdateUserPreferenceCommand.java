package com.bubli.user.dto;

import java.util.UUID;

public record UpdateUserPreferenceCommand(
		String theme,
		String defaultHomeType,
		UUID defaultProjectRoomId
) {
}
