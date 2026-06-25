package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateRoomTaskCommand(
		UUID assigneeUserId,
		UUID wbsItemId,
		String title,
		String description,
		TaskStatus status,
		Instant dueAt
) {
}
