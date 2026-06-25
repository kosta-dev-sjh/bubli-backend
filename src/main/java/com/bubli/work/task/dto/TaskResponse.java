package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
		UUID id,
		UUID ownerUserId,
		UUID assigneeUserId,
		UUID roomId,
		UUID wbsItemId,
		String title,
		String description,
		TaskStatus status,
		Instant dueAt,
		Instant createdAt,
		Instant updatedAt
) {
	public static TaskResponse from(TaskResult result) {
		return new TaskResponse(
				result.id(),
				result.ownerUserId(),
				result.assigneeUserId(),
				result.roomId(),
				result.wbsItemId(),
				result.title(),
				result.description(),
				result.status(),
				result.dueAt(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
