package com.bubli.work.task.dto;

import com.bubli.work.task.type.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreatePersonalTaskRequest(
		@NotBlank(message = "TODO 제목은 필수입니다.")
		@Size(max = 200, message = "TODO 제목은 200자 이하여야 합니다.")
		String title,

		String description,
		TaskStatus status,
		Instant dueAt
) {
}
