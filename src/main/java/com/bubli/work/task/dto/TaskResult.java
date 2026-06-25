package com.bubli.work.task.dto;

import com.bubli.work.task.entity.Task;
import com.bubli.work.task.type.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResult(
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
	public static TaskResult from(Task task) {
		return new TaskResult(
				task.getId(),
				task.getOwnerUserId(),
				task.getAssigneeUserId(),
				task.getRoomId(),
				task.getWbsItemId(),
				task.getTitle(),
				task.getDescription(),
				task.getStatus(),
				task.getDueAt(),
				task.getCreatedAt(),
				task.getUpdatedAt()
		);
	}
}
