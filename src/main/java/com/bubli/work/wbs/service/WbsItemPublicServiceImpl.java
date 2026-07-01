package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsItemResult;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WbsItemPublicServiceImpl implements WbsItemPublicService {

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
}
