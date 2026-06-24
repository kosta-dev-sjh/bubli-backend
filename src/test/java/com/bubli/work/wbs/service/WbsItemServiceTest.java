package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.dto.CreateWbsItemRequest;
import com.bubli.work.wbs.dto.UpdateWbsItemRequest;
import com.bubli.work.wbs.dto.WbsItemResult;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WbsItemServiceTest {

	@Mock
	WbsItemRepository wbsItemRepository;

	@Mock
	TaskRepository taskRepository;

	@Mock
	RoomMemberRepository roomMemberRepository;

	@InjectMocks
	WbsItemService wbsItemService;

	@Test
	void createWbsItemUsesNextOrderNoWhenRequestHasNoOrderNo() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
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
		));

		assertThat(result.id()).isEqualTo(itemId);
		assertThat(result.orderNo()).isEqualTo(4);
		assertThat(result.status()).isEqualTo(WbsStatus.TODO);
	}

	@Test
	void createWbsItemRejectsUserWithoutRoomAccess() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(false);

		assertThatThrownBy(() -> wbsItemService.create(userId, roomId, new CreateWbsItemRequest(
				null,
				"접근 불가",
				null,
				null
		))).isInstanceOf(BusinessException.class);
	}

	@Test
	void updateWbsItemChangesTitleOrderAndStatus() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "기존 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);

		WbsItemResult result = wbsItemService.update(userId, itemId, new UpdateWbsItemRequest(
				null,
				"수정 작업",
				2,
				WbsStatus.IN_PROGRESS
		));

		assertThat(result.title()).isEqualTo("수정 작업");
		assertThat(result.orderNo()).isEqualTo(2);
		assertThat(result.status()).isEqualTo(WbsStatus.IN_PROGRESS);
	}

	@Test
	void deleteWbsItemRejectsWhenTaskIsLinked() {
		UUID userId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		WbsItem item = WbsItem.create(roomId, null, "연결 작업", 1, WbsStatus.TODO);
		ReflectionTestUtils.setField(item, "id", itemId);
		given(wbsItemRepository.findById(itemId)).willReturn(Optional.of(item));
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(taskRepository.existsByWbsItemId(itemId)).willReturn(true);

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
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(taskRepository.existsByWbsItemId(itemId)).willReturn(false);
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
		given(roomMemberRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomMemberStatus.ACTIVE))
				.willReturn(true);
		given(taskRepository.existsByWbsItemId(itemId)).willReturn(false);

		wbsItemService.delete(userId, itemId);

		ArgumentCaptor<WbsItem> itemCaptor = ArgumentCaptor.forClass(WbsItem.class);
		verify(wbsItemRepository).delete(itemCaptor.capture());
		assertThat(itemCaptor.getValue().getId()).isEqualTo(itemId);
	}
}
