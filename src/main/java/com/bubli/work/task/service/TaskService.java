package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.work.task.dto.CreatePersonalTaskRequest;
import com.bubli.work.task.dto.CreateRoomTaskRequest;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.dto.UpdateTaskRequest;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
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
	private final RoomMemberRepository roomMemberRepository;
	private final WbsItemRepository wbsItemRepository;

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getPersonalTasks(UUID userId, Pageable pageable) {
		return toPage(taskRepository.findByOwnerUserIdAndRoomIdIsNull(userId, pageable));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getAssignedTasks(UUID userId, Pageable pageable) {
		return toPage(taskRepository.findByAssigneeUserId(userId, pageable));
	}

	@Transactional
	public TaskResult createPersonalTask(UUID userId, CreatePersonalTaskRequest request) {
		Task task = Task.createPersonal(
				userId,
				request.title(),
				request.description(),
				request.status(),
				request.dueAt()
		);
		return TaskResult.from(taskRepository.save(task));
	}

	@Transactional(readOnly = true)
	public PageResponse<TaskResult> getRoomTasks(UUID userId, UUID roomId, Pageable pageable) {
		checkRoomMember(userId, roomId);
		return toPage(taskRepository.findByRoomId(roomId, pageable));
	}

	@Transactional
	public TaskResult createRoomTask(UUID userId, UUID roomId, CreateRoomTaskRequest request) {
		checkRoomMember(userId, roomId);
		checkAssignee(roomId, request.assigneeUserId());
		checkWbsItem(roomId, request.wbsItemId());
		Task task = Task.createRoomTask(
				roomId,
				request.assigneeUserId(),
				request.wbsItemId(),
				request.title(),
				request.description(),
				request.status(),
				request.dueAt()
		);
		return TaskResult.from(taskRepository.save(task));
	}

	@Transactional
	public TaskResult updateTask(UUID userId, UUID taskId, UpdateTaskRequest request) {
		Task task = taskRepository.findById(taskId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_001));
		checkTaskAccess(userId, task);
		if (task.getRoomId() != null) {
			checkAssignee(task.getRoomId(), request.assigneeUserId());
			checkWbsItem(task.getRoomId(), request.wbsItemId());
		}
		task.update(
				request.title(),
				request.description(),
				request.assigneeUserId(),
				request.wbsItemId(),
				request.status(),
				request.dueAt()
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
		WbsItem wbsItem = wbsItemRepository.findById(wbsItemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_002));
		if (!roomId.equals(wbsItem.getRoomId())) {
			throw new BusinessException(ErrorCode.WORK_403_001);
		}
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
		boolean activeMember = roomMemberRepository.existsByRoomIdAndUserIdAndStatus(
				roomId,
				userId,
				RoomMemberStatus.ACTIVE
		);
		if (!activeMember) {
			throw new BusinessException(ErrorCode.PROJECT_403_001);
		}
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
