package com.bubli.work.wbs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderWbsItemsRequest(
		@Valid
		@NotEmpty(message = "WBS 순서 변경 항목은 필수입니다.")
		List<ReorderWbsItemRequest> items
) {
}
