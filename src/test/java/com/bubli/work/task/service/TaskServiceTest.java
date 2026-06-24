package com.bubli.work.task.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.work.task.dto.CreatePersonalTaskRequest;
import com.bubli.work.task.dto.CreateRoomTaskRequest;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.dto.UpdateTaskRequest;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	@Mock
	TaskRepository taskRepository;

	@Mock
	RoomMemberRepository roomMemberRepository;

	@Mock
	WbsItemRepository wbsItemRepository;

	@InjectMocks
	TaskService taskService;

	@Test
	void createPersonalTaskStoresOwnerAndDefaultStatus() {
		UUID userId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		CreatePersonalTaskRequest request = new CreatePersonalTaskRequest(
				"오늘 제안서 정리",
				"요구사항 확인",
				null,
				null
		);
		given(taskRepository.save(any(Task.class))).willAnswer(invocation -> {
			Task task = invocation.getArgument(0);
			ReflectionTestUtils.setField(task, "id", taskId);
			return task;
		});

		TaskResult result = taskService.createPersonalTask(userId, request);

		assertThat(result.id()).isEqualTo(taskId);
		assertThat(result.ownerUserId()).isEqualTo(userId);
		assertThat(result.roomId()).isNull();
		assertThat(result.status()).isEqualTo(TaskStatus.TODO);
	}

	@Test
	void createRoomTaskRequiresActiveRoomMemberAndValidWbsItem() {
		UUID userId = UUID.randomUUID();
		UUID assigneeUserId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID wbsItemId = UUID.randomUUID();
		WbsItem wbsItem = WbsItem.create(roomId, null, "디자인", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(wbsItem, "id", wbsItemId);
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, assigneeUserId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(wbsItemRepository.findById(wbsItemId)).willReturn(Optional.of(wbsItem));
		given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));
		CreateRoomTaskRequest request = new CreateRoomTaskRequest(
				assigneeUserId,
				wbsItemId,
				"시안 정리",
				null,
				TaskStatus.IN_PROGRESS,
				null
		);

		TaskResult result = taskService.createRoomTask(userId, roomId, request);

		assertThat(result.roomId()).isEqualTo(roomId);
		assertThat(result.assigneeUserId()).isEqualTo(assigneeUserId);
		assertThat(result.wbsItemId()).isEqualTo(wbsItemId);
		assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
	}

	@Test
	void updatePersonalTaskRejectsOtherUser() {
		UUID ownerUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		UUID taskId = UUID.randomUUID();
		Task task = Task.createPersonal(ownerUserId, "개인 할 일", null, TaskStatus.TODO, null);
		ReflectionTestUtils.setField(task, "id", taskId);
		given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

		assertThatThrownBy(() -> taskService.updateTask(
				otherUserId,
				taskId,
				new UpdateTaskRequest("수정", null, null, null, null, null)
		)).isInstanceOf(BusinessException.class);
	}

	@Test
	void createRoomTaskSavesRoomTask() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(taskRepository.save(any(Task.class))).willAnswer(invocation -> invocation.getArgument(0));

		taskService.createRoomTask(userId, roomId, new CreateRoomTaskRequest(
				null,
				null,
				"계약서 확인",
				null,
				null,
				null
		));

		ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
		verify(taskRepository).save(taskCaptor.capture());
		assertThat(taskCaptor.getValue().getRoomId()).isEqualTo(roomId);
		assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
	}
}
