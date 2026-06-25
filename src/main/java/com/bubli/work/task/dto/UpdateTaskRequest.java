package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record UpdateTaskRequest(
		@Size(max = 200, message = "TODO 제목은 200자 이하여야 합니다.")
		String title,
		String description,
		UUID assigneeUserId,
		UUID wbsItemId,
		TaskStatus status,
		Instant dueAt
) {
	public UpdateTaskCommand toCommand() {
		return new UpdateTaskCommand(title, description, assigneeUserId, wbsItemId, status, dueAt);
	}
}
