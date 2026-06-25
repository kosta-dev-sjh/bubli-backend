package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.type.WbsStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateWbsItemRequest(
		UUID parentId,

		@Size(max = 200, message = "WBS 항목명은 200자 이하여야 합니다.")
		String title,

		Integer orderNo,
		WbsStatus status
) {
	public UpdateWbsItemCommand toCommand() {
		return new UpdateWbsItemCommand(parentId, title, orderNo, status);
	}
}
