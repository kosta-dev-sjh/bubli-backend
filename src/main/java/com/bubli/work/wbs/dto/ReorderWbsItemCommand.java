package com.bubli.work.wbs.dto;

import java.util.UUID;

public record ReorderWbsItemCommand(
		UUID wbsItemId,
		UUID parentId,
		Integer orderNo
) {
}
