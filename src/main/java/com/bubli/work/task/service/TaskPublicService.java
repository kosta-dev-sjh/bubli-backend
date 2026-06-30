package com.bubli.work.task.service;

import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.dto.CreateRoomTaskCommand;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskPublicService {

	List<TaskResult> getRoomTasksForBoard(UUID roomId);

	List<TaskResult> getRecentRoomTasks(UUID roomId, int limit);

	List<TaskResult> getPersonalContextTasks(UUID userId, int limit);

	List<TaskResult> getDueBetweenTasks(UUID userId, Instant from, Instant to);

	boolean existsByWbsItemId(UUID wbsItemId);

	void assertNoTaskLinkedToWbsItem(UUID wbsItemId);

	TaskResult createRoomTask(UUID userId, UUID roomId, CreateRoomTaskCommand command);
}
