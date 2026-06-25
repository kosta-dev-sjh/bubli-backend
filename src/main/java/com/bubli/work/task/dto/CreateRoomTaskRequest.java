package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateRoomTaskRequest(
		UUID assigneeUserId,
		UUID wbsItemId,

		@NotBlank(message = "TODO 제목은 필수입니다.")
		@Size(max = 200, message = "TODO 제목은 200자 이하여야 합니다.")
		String title,

		String description,
		TaskStatus status,
		Instant dueAt
) {
	public CreateRoomTaskCommand toCommand() {
		return new CreateRoomTaskCommand(assigneeUserId, wbsItemId, title, description, status, dueAt);
	}
}
