package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.task.dto.CreatePersonalTaskCommand;
import com.bubli.work.task.dto.CreateRoomTaskCommand;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.dto.UpdateTaskCommand;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.service.WbsItemPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

	private final TaskRepository taskRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;
	private final WbsItemPublicService wbsItemPublicService;

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getPersonalTasks(UUID userId, Pageable pageable) {
		return toPage(taskRepository.findByOwnerUserIdAndRoomIdIsNull(userId, pageable));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getAssignedTasks(UUID userId, Pageable pageable) {
		return toPage(taskRepository.findByAssigneeUserId(userId, pageable));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getDashboardTasks(UUID userId, Pageable pageable) {
		return toPage(taskRepository.findDashboardTasks(userId, pageable));
	}

	@Transactional
	public TaskResult createPersonalTask(UUID userId, CreatePersonalTaskCommand command) {
		Task task = Task.createPersonal(
				userId,
				command.title(),
				command.description(),
				command.status(),
				command.dueAt()
		);
		return TaskResult.from(taskRepository.save(task));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getRoomTasks(UUID userId, UUID roomId, Pageable pageable) {
		checkRoomMember(userId, roomId);
		return toPage(taskRepository.findByRoomId(roomId, pageable));
	}

	@Transactional
	public TaskResult createRoomTask(UUID userId, UUID roomId, CreateRoomTaskCommand command) {
		checkRoomMember(userId, roomId);
		checkAssignee(roomId, command.assigneeUserId());
		checkWbsItem(roomId, command.wbsItemId());
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

	@Transactional
	public TaskResult updateTask(UUID userId, UUID taskId, UpdateTaskCommand command) {
		Task task = taskRepository.findById(taskId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_001));
		checkTaskAccess(userId, task);
		if (task.getRoomId() != null) {
			checkAssignee(task.getRoomId(), command.assigneeUserId());
			checkWbsItem(task.getRoomId(), command.wbsItemId());
		}
		task.update(
				command.title(),
				command.description(),
				command.assigneeUserId(),
				command.wbsItemId(),
				command.status(),
				command.dueAt()
		);
		return TaskResult.from(task);
	}

	@Transactional
	public void deleteTask(UUID userId, UUID taskId) {
		Task task = taskRepository.findById(taskId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_001));
		checkTaskAccess(userId, task);
		taskRepository.delete(task);
	}

	private void checkAssignee(UUID roomId, UUID assigneeUserId) {
		if (assigneeUserId == null) {
			return;
		}
		checkRoomMember(assigneeUserId, roomId);
	}

	private void checkWbsItem(UUID roomId, UUID wbsItemId) {
		if (wbsItemId == null) {
			return;
		}
		wbsItemPublicService.assertRoomWbsItem(roomId, wbsItemId);
	}

	private void checkTaskAccess(UUID userId, Task task) {
		if (task.getRoomId() != null) {
			checkRoomMember(userId, task.getRoomId());
			return;
		}
		if (!userId.equals(task.getOwnerUserId())) {
			throw new BusinessException(ErrorCode.WORK_403_001);
		}
	}

	private void checkRoomMember(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
	}

	private PageResponse<TaskResult> toPage(Page<Task> page) {
		return new PageResponse<>(
				page.getContent().stream()
						.map(TaskResult::from)
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}
}
