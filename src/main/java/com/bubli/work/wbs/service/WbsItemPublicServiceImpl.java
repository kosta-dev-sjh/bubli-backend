package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WbsItemPublicServiceImpl implements WbsItemPublicService {

	private static final int WBS_ORDER_SAVE_MAX_ATTEMPTS = 3;

	private final WbsItemRepository wbsItemRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Override
	@Transactional(readOnly = true)
	public void assertRoomWbsItem(UUID roomId, UUID wbsItemId) {
		if (wbsItemId == null) {
			return;
		}
		WbsItem wbsItem = wbsItemRepository.findById(wbsItemId)
				.orElseThrow(() -> new BusinessException(ErrorCode.WORK_404_002));
		if (!roomId.equals(wbsItem.getRoomId())) {
			throw new BusinessException(ErrorCode.WORK_403_001);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<WbsItemResult> getRoomItemsForBoard(UUID roomId) {
		return wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId).stream()
				.map(WbsItemResult::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<WbsItemResult> getRoomContextItems(UUID roomId, int limit) {
		return wbsItemRepository.findByRoomIdOrderByParentIdAscOrderNoAsc(roomId).stream()
				.limit(Math.max(1, Math.min(limit, 20)))
				.map(WbsItemResult::from)
				.toList();
	}

	@Override
	@Transactional
	public WbsItemResult create(UUID userId, UUID roomId, CreateWbsItemCommand command) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		assertRoomWbsItem(roomId, command.parentId());
		if (command.orderNo() != null) {
			return WbsItemResult.from(createExplicitOrderItem(roomId, command));
		}
		return WbsItemResult.from(createAutoOrderItemWithRetry(roomId, command));
	}

	private WbsItem createExplicitOrderItem(UUID roomId, CreateWbsItemCommand command) {
		if (wbsItemRepository.existsSiblingOrder(roomId, command.parentId(), command.orderNo())) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
		try {
			return saveItem(roomId, command, command.orderNo());
		} catch (DataIntegrityViolationException exception) {
			throw new BusinessException(ErrorCode.COMMON_400_002);
		}
	}

	private WbsItem createAutoOrderItemWithRetry(UUID roomId, CreateWbsItemCommand command) {
		DataIntegrityViolationException lastException = null;
		for (int attempt = 0; attempt < WBS_ORDER_SAVE_MAX_ATTEMPTS; attempt++) {
			try {
				int orderNo = wbsItemRepository.findMaxOrderNo(roomId, command.parentId()) + 1;
				return saveItem(roomId, command, orderNo);
			} catch (DataIntegrityViolationException exception) {
				lastException = exception;
			}
		}
		if (lastException == null) {
			throw new IllegalStateException("WBS order save retry attempts must be positive.");
		}
		throw lastException;
	}

	private WbsItem saveItem(UUID roomId, CreateWbsItemCommand command, int orderNo) {
		WbsItem item = WbsItem.create(
				roomId,
				command.parentId(),
				command.title(),
				orderNo,
				command.status()
		);
		return wbsItemRepository.saveAndFlush(item);
	}
}
