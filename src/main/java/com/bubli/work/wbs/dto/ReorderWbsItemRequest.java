package com.bubli.work.wbs.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ReorderWbsItemRequest(
		@NotNull(message = "WBS 항목 ID는 필수입니다.")
		UUID wbsItemId,

		UUID parentId,

		@NotNull(message = "WBS 순서는 필수입니다.")
		@Positive(message = "WBS 순서는 1 이상이어야 합니다.")
		Integer orderNo
) {
	public ReorderWbsItemCommand toCommand() {
		return new ReorderWbsItemCommand(wbsItemId, parentId, orderNo);
	}
}
