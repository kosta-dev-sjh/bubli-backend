package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.task.dto.CreateRoomTaskCommand;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.service.WbsItemPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskPublicServiceImpl implements TaskPublicService {

	private final TaskRepository taskRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final WbsItemPublicService wbsItemPublicService;

	@Override
	@Transactional(readOnly = true)
	public List<TaskResult> getRoomTasksForBoard(UUID roomId) {
		return taskRepository.findByRoomIdOrderByUpdatedAtDesc(roomId).stream()
				.map(TaskResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskResult> getDueBetweenTasks(UUID userId, Instant from, Instant to) {
		return taskRepository.findDueBetweenForUser(userId, from, to).stream()
				.map(TaskResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByWbsItemId(UUID wbsItemId) {
		return taskRepository.existsByWbsItemId(wbsItemId);
	}

	@Override
	@Transactional(readOnly = true)
	public void assertNoTaskLinkedToWbsItem(UUID wbsItemId) {
		if (existsByWbsItemId(wbsItemId)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
	}

	@Override
	@Transactional
	public TaskResult createRoomTask(UUID userId, UUID roomId, CreateRoomTaskCommand command) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		if (command.assigneeUserId() != null) {
			projectMembershipPublicService.assertActiveMember(command.assigneeUserId(), roomId);
		}
		wbsItemPublicService.assertRoomWbsItem(roomId, command.wbsItemId());
		Task task = Task.createRoomTask(
				roomId,
				command.assigneeUserId(),
				command.wbsItemId(),
				command.title(),
				command.description(),
				command.status(),
				command.dueAt()
		);
		return TaskResult.from(taskRepository.save(task));
	}
}
