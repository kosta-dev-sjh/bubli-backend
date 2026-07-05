package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.type.WbsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateWbsItemRequest(
		UUID parentId,

		@NotBlank(message = "WBS 항목명은 필수입니다.")
		@Size(max = 200, message = "WBS 항목명은 200자 이하여야 합니다.")
		String title,

		Integer orderNo,
		WbsStatus status,

		@Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
		String scheduleTitle,

		Instant startsAt,
		Instant dueAt,
		Instant endsAt,
		Boolean allDay
) {
	public CreateWbsItemRequest(UUID parentId, String title, Integer orderNo, WbsStatus status) {
		this(parentId, title, orderNo, status, null, null, null, null, false);
	}

	public CreateWbsItemCommand toCommand() {
		return new CreateWbsItemCommand(
				parentId,
				title,
				orderNo,
				status,
				scheduleTitle == null ? null : scheduleTitle.trim(),
				startsAt,
				dueAt,
				endsAt,
				Boolean.TRUE.equals(allDay)
		);
	}
}
