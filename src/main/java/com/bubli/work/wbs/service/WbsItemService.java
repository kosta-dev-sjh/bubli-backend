package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.task.service.TaskPublicService;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.ReorderWbsItemCommand;
import com.bubli.work.wbs.dto.ReorderWbsItemsCommand;
import com.bubli.work.wbs.dto.UpdateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsBoardResult;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WbsItemService {

	private final WbsItemRepository wbsItemRepository;
	private final TaskPublicService taskPublicService;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional(readOnly = true)
	public PageResponse<WbsItemResult> getRoomWbsItems(UUID userId, UUID roomId, Pageable pageable) {
		checkRoomMember(userId, roomId);
		return toPage(wbsItemRepository.findByRoomIdOrderByOrderNoAsc(roomId, pageable));
	}

	@Transactional(readOnly = true)
	public WbsBoardResult getWbsBoard(UUID userId, UUID roomId) {
		checkRoomMember(userId, roomId);
		return new WbsBoardResult(
				wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId).stream()
						.map(WbsItemResult::from)
						.toList(),
				taskPublicService.getRoomTasksForBoard(roomId)
		);
	}

	@Transactional
	public WbsItemResult create(UUID userId, UUID roomId, CreateWbsItemCommand command) {
		checkRoomMember(userId, roomId);
		checkParent(roomId, command.parentId());
		int orderNo = command.orderNo() == null
				? wbsItemRepository.findMaxOrderNo(roomId, command.parentId()) + 1
				: command.orderNo();
		WbsItem item = WbsItem.create(
				roomId,
				command.parentId(),
				command.title(),
				orderNo,
				command.status()
		);
		return WbsItemResult.from(wbsItemRepository.save(item));
	}

	@Transactional
	public WbsItemResult update(UUID userId, UUID itemId, UpdateWbsItemCommand command) {
		WbsItem item = getItem(itemId);
		checkRoomMember(userId, item.getRoomId());
		checkParent(item.getRoomId(), command.parentId());
		if (item.getId().equals(command.parentId())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		item.update(
				command.title(),
				command.parentId(),
				command.orderNo(),
				command.status()
		);
		return WbsItemResult.from(item);
	}

	@Transactional
	public List<WbsItemResult> reorder(UUID userId, UUID roomId, ReorderWbsItemsCommand command) {
		checkRoomMember(userId, roomId);
		List<WbsItem> roomItems = wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId);
		Map<UUID, WbsItem> roomItemById = roomItems.stream()
				.collect(Collectors.toMap(WbsItem::getId, Function.identity()));
		Map<UUID, ReorderWbsItemCommand> requestByItemId = toRequestMap(command.items(), roomItemById);
		validateReorder(roomItems, requestByItemId, roomItemById);

		List<WbsItem> targets = requestByItemId.keySet().stream()
				.map(roomItemById::get)
				.toList();
		for (int index = 0; index < targets.size(); index++) {
			WbsItem item = targets.get(index);
			item.reorder(item.getParentId(), -(index + 1));
		}
		wbsItemRepository.saveAllAndFlush(targets);

		requestByItemId.forEach((itemId, reorderRequest) ->
				roomItemById.get(itemId).reorder(reorderRequest.parentId(), reorderRequest.orderNo()));
		wbsItemRepository.saveAllAndFlush(targets);

		return wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId).stream()
				.map(WbsItemResult::from)
				.toList();
	}

	@Transactional
	public void delete(UUID userId, UUID itemId) {
		WbsItem item = getItem(itemId);
		checkRoomMember(userId, item.getRoomId());
		taskPublicService.assertNoTaskLinkedToWbsItem(itemId);
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
		projectMembershipPublicService.assertActiveMember(userId, roomId);
	}

	private Map<UUID, ReorderWbsItemCommand> toRequestMap(
			List<ReorderWbsItemCommand> requests,
			Map<UUID, WbsItem> roomItemById
	) {
		Map<UUID, ReorderWbsItemCommand> requestByItemId = new LinkedHashMap<>();
		for (ReorderWbsItemCommand request : requests) {
			if (!roomItemById.containsKey(request.wbsItemId())) {
				throw new BusinessException(ErrorCode.WORK_404_002);
			}
			if (requestByItemId.put(request.wbsItemId(), request) != null) {
				throw new BusinessException(ErrorCode.COMMON_400_002);
			}
		}
		return requestByItemId;
	}

	private void validateReorder(
			List<WbsItem> roomItems,
			Map<UUID, ReorderWbsItemCommand> requestByItemId,
			Map<UUID, WbsItem> roomItemById
	) {
		Set<String> siblingOrders = new HashSet<>();
		for (WbsItem item : roomItems) {
			ReorderWbsItemCommand request = requestByItemId.get(item.getId());
			UUID parentId = request == null ? item.getParentId() : request.parentId();
			Integer orderNo = request == null ? item.getOrderNo() : request.orderNo();
			if (item.getId().equals(parentId)) {
				throw new BusinessException(ErrorCode.COMMON_400_002);
			}
			if (parentId != null && !roomItemById.containsKey(parentId)) {
				throw new BusinessException(ErrorCode.WORK_403_001);
			}
			if (!siblingOrders.add(parentId + ":" + orderNo)) {
				throw new BusinessException(ErrorCode.COMMON_400_002);
			}
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
