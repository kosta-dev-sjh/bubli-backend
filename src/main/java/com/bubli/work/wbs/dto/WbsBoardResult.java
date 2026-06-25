package com.bubli.work.wbs.dto;

import com.bubli.work.task.dto.TaskResult;

import java.util.List;

public record WbsBoardResult(
		List<WbsItemResult> wbsItems,
		List<TaskResult> tasks
) {
}
