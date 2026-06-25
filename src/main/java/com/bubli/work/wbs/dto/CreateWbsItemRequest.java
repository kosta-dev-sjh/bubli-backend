package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.type.WbsStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateWbsItemRequest(
		UUID parentId,

		@NotBlank(message = "WBS 항목명은 필수입니다.")
		@Size(max = 200, message = "WBS 항목명은 200자 이하여야 합니다.")
		String title,

		Integer orderNo,
		WbsStatus status
) {
	public CreateWbsItemCommand toCommand() {
		return new CreateWbsItemCommand(parentId, title, orderNo, status);
	}
}
