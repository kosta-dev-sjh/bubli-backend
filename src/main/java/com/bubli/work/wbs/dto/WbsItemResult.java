package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.entity.WbsItem;
import com.bubli.work.wbs.type.WbsStatus;

import java.time.Instant;
import java.util.UUID;

public record WbsItemResult(
		UUID id,
		UUID roomId,
		UUID parentId,
		String title,
		Integer orderNo,
		WbsStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static WbsItemResult from(WbsItem item) {
		return new WbsItemResult(
				item.getId(),
				item.getRoomId(),
				item.getParentId(),
				item.getTitle(),
				item.getOrderNo(),
				item.getStatus(),
				item.getCreatedAt(),
				item.getUpdatedAt()
		);
	}
}
