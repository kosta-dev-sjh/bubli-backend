package com.bubli.work.wbs.dto;

import com.bubli.work.task.dto.TaskResponse;

import java.util.List;

public record WbsBoardResponse(
		List<WbsItemResponse> wbsItems,
		List<TaskResponse> tasks
) {
	public static WbsBoardResponse from(WbsBoardResult result) {
		return new WbsBoardResponse(
				result.wbsItems().stream()
						.map(WbsItemResponse::from)
						.toList(),
				result.tasks().stream()
						.map(TaskResponse::from)
						.toList()
		);
	}
}
