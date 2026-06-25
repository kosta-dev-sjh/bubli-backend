package com.bubli.work.wbs.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.repository.WbsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WbsItemPublicService {

	private final WbsItemRepository wbsItemRepository;

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
}
