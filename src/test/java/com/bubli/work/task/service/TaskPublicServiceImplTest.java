package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.service.WbsItemPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskPublicServiceImplTest {

	@Mock
	TaskRepository taskRepository;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	WbsItemPublicService wbsItemPublicService;

	@InjectMocks
	TaskPublicServiceImpl taskPublicService;

	@Test
	void assertScheduleTaskScopeAcceptsAssignedRoomTaskForSameRoom() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Task task = Task.createRoomTask(roomId, userId, null, "룸 TODO", null, null, null);
		given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

		taskPublicService.assertScheduleTaskScope(userId, roomId, taskId);

		verify(projectMembershipPublicService).assertActiveMember(userId, roomId);
	}

	@Test
	void assertScheduleTaskScopeRejectsRoomTaskAsPersonalSchedule() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Task task = Task.createRoomTask(roomId, userId, null, "룸 TODO", null, null, null);
		given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

		assertThatThrownBy(() -> taskPublicService.assertScheduleTaskScope(userId, null, taskId))
				.isInstanceOf(BusinessException.class);
	}
}
