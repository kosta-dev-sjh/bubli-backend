package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.schedule.dto.CreateScheduleCommand;
import com.bubli.work.schedule.service.SchedulePublicService;
import com.bubli.work.task.dto.TaskResult;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.dto.ApplyWbsToCalendarCommand;
import com.bubli.work.wbs.dto.CreateWbsItemRequest;
import com.bubli.work.wbs.dto.ReorderWbsItemRequest;
import com.bubli.work.wbs.dto.ReorderWbsItemsRequest;
import com.bubli.work.wbs.dto.UpdateWbsItemRequest;
import com.bubli.work.wbs.dto.WbsBoardResult;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import com.bubli.work.task.type.TaskStatus;
import com.bubli.work.wbs.type.WbsStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WbsItemServiceTest {

	@Mock
	WbsItemRepository wbsItemRepository;

	@Mock
	TaskPublicService taskPublicService;

	@Mock
	ProjectMembershipPublicService projectMembershipPublicService;

	@Mock
	SchedulePublicService schedulePublicService;

	@InjectMocks
	WbsItemService wbsItemService;

	@Test
	void getWbsBoardReturnsRoomWbsItemsAndTasks() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "요구사항 정리", 1, WbsStatus.TODO);
		Task task = Task.createRoomTask(roomId, userId, null, "계약서 확인", null, TaskStatus.TODO, null);
		given(wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId)).willReturn(List.of(item));
		given(taskPublicService.getRoomTasksForBoard(roomId)).willReturn(List.of(TaskResult.from(task)));

		WbsBoardResult result = wbsItemService.getWbsBoard(userId, roomId);

		assertThat(result.wbsItems()).hasSize(1);
		assertThat(result.wbsItems().get(0).title()).isEqualTo("요구사항 정리");
		assertThat(result.tasks()).hasSize(1);
		assertThat(result.tasks().get(0).title()).isEqualTo("계약서 확인");
	}

	@Test
	void createWbsItemUsesNextOrderNoWhenRequestHasNoOrderNo() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		given(wbsItemRepository.findMaxOrderNo(roomId, null)).willReturn(3);
		given(wbsItemRepository.save(any(WbsItem.class))).willAnswer(invocation -> {
			WbsItem item = invocation.getArgument(0);
			ReflectionTestUtils.setField(item, "id", itemId);
			return item;
		});

		WbsItemResult result = wbsItemService.create(userId, roomId, new CreateWbsItemRequest(
				null,
				"퍼블리싱",
				null,
				null
		).toCommand());

		assertThat(result.id()).isEqualTo(itemId);
		assertThat(result.orderNo()).isEqualTo(4);
		assertThat(result.status()).isEqualTo(WbsStatus.TODO);
	}

	@Test
	void createWbsItemRejectsUserWithoutRoomAccess() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		willThrow(new BusinessException(ErrorCode.PROJECT_403_001))
				.given(projectMembershipPublicService)
				.assertActiveMember(userId, roomId);

		assertThatThrownBy(() -> wbsItemService.create(userId, roomId, new CreateWbsItemRequest(
				null,
				"접근 불가",
				null,
				null
		).toCommand())).isInstanceOf(BusinessException.class);
	}

	@Test
	void updateWbsItemChangesTitleOrderAndStatus() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "기존 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));

		WbsItemResult result = wbsItemService.update(userId, itemId, new UpdateWbsItemRequest(
				null,
				"수정 작업",
				2,
				WbsStatus.IN_PROGRESS
		).toCommand());

		assertThat(result.title()).isEqualTo("수정 작업");
		assertThat(result.orderNo()).isEqualTo(2);
		assertThat(result.status()).isEqualTo(WbsStatus.IN_PROGRESS);
	}

	@Test
	void reorderWbsItemsChangesSiblingOrder() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		WbsItem first = WbsItem.create(roomId, null, "첫 번째", 1, WbsStatus.TODO);
		WbsItem second = WbsItem.create(roomId, null, "두 번째", 2, WbsStatus.TODO);
		ReflectionTestUtils.setField(first, "id", firstId);
		ReflectionTestUtils.setField(second, "id", secondId);
		given(wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId))
				.willReturn(List.of(first, second));

		List<WbsItemResult> results = wbsItemService.reorder(userId, roomId, new ReorderWbsItemsRequest(List.of(
				new ReorderWbsItemRequest(firstId, null, 2),
				new ReorderWbsItemRequest(secondId, null, 1)
		)).toCommand());

		assertThat(results).hasSize(2);
		assertThat(first.getOrderNo()).isEqualTo(2);
		assertThat(second.getOrderNo()).isEqualTo(1);
	}

	@Test
	void reorderWbsItemsRejectsDuplicatedSiblingOrder() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		WbsItem first = WbsItem.create(roomId, null, "첫 번째", 1, WbsStatus.TODO);
		WbsItem second = WbsItem.create(roomId, null, "두 번째", 2, WbsStatus.TODO);
		ReflectionTestUtils.setField(first, "id", firstId);
		ReflectionTestUtils.setField(second, "id", secondId);
		given(wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId))
				.willReturn(List.of(first, second));

		assertThatThrownBy(() -> wbsItemService.reorder(userId, roomId, new ReorderWbsItemsRequest(List.of(
				new ReorderWbsItemRequest(firstId, null, 1),
				new ReorderWbsItemRequest(secondId, null, 1)
		)).toCommand())).isInstanceOf(BusinessException.class);
	}

	@Test
	void deleteWbsItemRejectsWhenTaskIsLinked() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "연결 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));
		willThrow(new BusinessException(ErrorCode.COMMON_400_002))
				.given(taskPublicService)
				.assertNoTaskLinkedToWbsItem(itemId);

		assertThatThrownBy(() -> wbsItemService.delete(userId, itemId))
				.isInstanceOf(BusinessException.class);
		verify(wbsItemRepository, never()).delete(any(WbsItem.class));
	}

	@Test
	void deleteWbsItemRejectsWhenChildItemExists() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "상위 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));
		given(wbsItemRepository.existsByParentId(itemId)).willReturn(true);

		assertThatThrownBy(() -> wbsItemService.delete(userId, itemId))
				.isInstanceOf(BusinessException.class);
		verify(wbsItemRepository, never()).delete(any(WbsItem.class));
	}

	@Test
	void deleteWbsItemDeletesWhenNoTaskIsLinked() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "삭제 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));

		wbsItemService.delete(userId, itemId);

		ArgumentCaptor<WbsItem> itemCaptor = ArgumentCaptor.forClass(WbsItem.class);
		verify(wbsItemRepository).delete(itemCaptor.capture());
		assertThat(itemCaptor.getValue().getId()).isEqualTo(itemId);
	}

	@Test
	void applyToCalendarCreatesSchedulesForRoomWbsItems() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		Instant startsAt = Instant.parse("2026-07-03T01:00:00Z");
		WbsItem first = WbsItem.create(roomId, null, "기획 정리", 1, WbsStatus.TODO);
		WbsItem second = WbsItem.create(roomId, null, "화면 구현", 2, WbsStatus.IN_PROGRESS);
		ReflectionTestUtils.setField(first, "id", firstId);
		ReflectionTestUtils.setField(second, "id", secondId);
		given(wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId))
				.willReturn(List.of(first, second));

		wbsItemService.applyToCalendar(userId, roomId, new ApplyWbsToCalendarCommand(
				startsAt,
				null,
				30,
				10,
				false,
				true,
				null
		));

		ArgumentCaptor<CreateScheduleCommand> commandCaptor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		verify(schedulePublicService, times(2)).create(eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getAllValues())
				.extracting(CreateScheduleCommand::wbsItemId)
				.containsExactly(firstId, secondId);
		assertThat(commandCaptor.getAllValues())
				.extracting(CreateScheduleCommand::title)
				.containsExactly("기획 정리", "화면 구현");
		assertThat(commandCaptor.getAllValues().get(0).startsAt()).isEqualTo(startsAt);
		assertThat(commandCaptor.getAllValues().get(0).endsAt()).isEqualTo(Instant.parse("2026-07-03T01:30:00Z"));
		assertThat(commandCaptor.getAllValues().get(1).startsAt()).isEqualTo(Instant.parse("2026-07-03T01:40:00Z"));
		assertThat(commandCaptor.getAllValues().get(1).endsAt()).isEqualTo(Instant.parse("2026-07-03T02:10:00Z"));
	}

	@Test
	void applyToCalendarDistributesWbsItemsAcrossRequestedRange() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID firstId = UUID.randomUUID();
		UUID secondId = UUID.randomUUID();
		WbsItem first = WbsItem.create(roomId, null, "계약서 내용 검토", 1, WbsStatus.TODO);
		WbsItem second = WbsItem.create(roomId, null, "보완 사항 반영", 2, WbsStatus.TODO);
		ReflectionTestUtils.setField(first, "id", firstId);
		ReflectionTestUtils.setField(second, "id", secondId);
		given(wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId))
				.willReturn(List.of(first, second));

		wbsItemService.applyToCalendar(userId, roomId, new ApplyWbsToCalendarCommand(
				Instant.parse("2026-07-03T00:00:00Z"),
				Instant.parse("2026-07-09T00:00:00Z"),
				null,
				0,
				false,
				true,
				null
		));

		ArgumentCaptor<CreateScheduleCommand> commandCaptor = ArgumentCaptor.forClass(CreateScheduleCommand.class);
		verify(schedulePublicService, times(2)).create(eq(userId), commandCaptor.capture());
		assertThat(commandCaptor.getAllValues().get(0).startsAt()).isEqualTo(Instant.parse("2026-07-03T00:00:00Z"));
		assertThat(commandCaptor.getAllValues().get(0).endsAt()).isEqualTo(Instant.parse("2026-07-06T00:00:00Z"));
		assertThat(commandCaptor.getAllValues().get(1).startsAt()).isEqualTo(Instant.parse("2026-07-06T00:00:00Z"));
		assertThat(commandCaptor.getAllValues().get(1).endsAt()).isEqualTo(Instant.parse("2026-07-09T00:00:00Z"));
	}
}
