package com.bubli.work.task.service;

import com.bubli.work.task.dto.TaskResult;

import java.util.List;
import java.util.UUID;

public interface TaskPublicService {

	List<TaskResult> getRoomTasksForBoard(UUID roomId);

	boolean existsByWbsItemId(UUID wbsItemId);

	void assertNoTaskLinkedToWbsItem(UUID wbsItemId);
}
