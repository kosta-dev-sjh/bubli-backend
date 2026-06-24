package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.project.type.RoomMemberStatus;
import com.bubli.work.task.repository.TaskRepository;
import com.bubli.work.wbs.dto.CreateWbsItemRequest;
import com.bubli.work.wbs.dto.UpdateWbsItemRequest;
import com.bubli.work.wbs.dto.WbsItemResult;
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
public class WbsItemService {

	private final WbsItemRepository wbsItemRepository;
	private final TaskRepository taskRepository;
	private final RoomMemberRepository roomMemberRepository;

	@Transactional(readOnly = true)
	public PageResponse<WbsItemResult> getRoomWbsItems(UUID userId, UUID roomId, Pageable pageable) {
		checkRoomMember(userId, roomId);
		return toPage(wbsItemRepository.findByRoomIdOrderByOrderNoAsc(roomId, pageable));
	}

	@Transactional
	public WbsItemResult create(UUID userId, UUID roomId, CreateWbsItemRequest request) {
		checkRoomMember(userId, roomId);
		checkParent(roomId, request.parentId());
		int orderNo = request.orderNo() == null
				? wbsItemRepository.findMaxOrderNo(roomId, request.parentId()) + 1
				: request.orderNo();
		WbsItem item = WbsItem.create(
				roomId,
				request.parentId(),
				request.title(),
				orderNo,
				request.status()
		);
		return WbsItemResult.from(wbsItemRepository.save(item));
	}

	@Transactional
	public WbsItemResult update(UUID userId, UUID itemId, UpdateWbsItemRequest request) {
		WbsItem item = getItem(itemId);
		checkRoomMember(userId, item.getRoomId());
		checkParent(item.getRoomId(), request.parentId());
		if (item.getId().equals(request.parentId())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		item.update(
				request.title(),
				request.parentId(),
				request.orderNo(),
				request.status()
		);
		return WbsItemResult.from(item);
	}

	@Transactional
	public void delete(UUID userId, UUID itemId) {
		WbsItem item = getItem(itemId);
		checkRoomMember(userId, item.getRoomId());
		if (taskRepository.existsByWbsItemId(itemId)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		if (wbsItemRepository.existsByParentId(itemId)) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		wbsItemRepository.delete(item);
	}

	private WbsItem getItem(UUID itemId) {
		return wbsItemRepository.findById(itemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_002));
	}

	private void checkParent(UUID roomId, UUID parentId) {
		if (parentId == null) {
			return;
		}
		WbsItem parent = getItem(parentId);
		if (!roomId.equals(parent.getRoomId())) {
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

	private PageResponse<WbsItemResult> toPage(Page<WbsItem> page) {
		return new PageResponse<>(
				page.getContent().stream()
						.map(WbsItemResult::from)
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}
}
