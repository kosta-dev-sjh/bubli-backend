package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record UpdateTaskCommand(
		String title,
		String description,
		UUID assigneeUserId,
		UUID wbsItemId,
		TaskStatus status,
		Instant dueAt
) {
}
