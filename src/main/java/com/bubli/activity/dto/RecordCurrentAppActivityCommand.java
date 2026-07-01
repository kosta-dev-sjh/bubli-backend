package com.bubli.activity.dto;

import java.time.Instant;
import java.util.UUID;

public record RecordCurrentAppActivityCommand(
		UUID roomId,
		String appName,
		String windowTitle,
		Instant startedAt,
		Instant endedAt,
		Long durationSeconds
) {
}
