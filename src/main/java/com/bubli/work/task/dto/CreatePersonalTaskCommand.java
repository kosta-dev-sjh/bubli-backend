package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;

import java.time.Instant;

public record CreatePersonalTaskCommand(
		String title,
		String description,
		TaskStatus status,
		Instant dueAt
) {
}
