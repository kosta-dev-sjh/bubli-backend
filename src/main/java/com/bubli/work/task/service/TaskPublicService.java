package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskPublicService {

	private final TaskRepository taskRepository;

	@Transactional(readOnly = true)
	public List<TaskResult> getRoomTasksForBoard(UUID roomId) {
		return taskRepository.findByRoomIdOrderByUpdatedAtDesc(roomId).stream()
				.map(TaskResult::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean existsByWbsItemId(UUID wbsItemId) {
		return taskRepository.existsByWbsItemId(wbsItemId);
	}

	@Transactional(readOnly = true)
	public void assertNoTaskLinkedToWbsItem(UUID wbsItemId) {
		if (existsByWbsItemId(wbsItemId)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
	}
}
