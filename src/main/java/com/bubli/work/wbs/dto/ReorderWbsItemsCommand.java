package com.bubli.work.wbs.dto;

import java.util.List;

public record ReorderWbsItemsCommand(
		List<ReorderWbsItemCommand> items
) {
}
