package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.type.WbsStatus;

import java.time.Instant;
import java.util.UUID;

public record WbsItemResponse(
		UUID id,
		UUID roomId,
		UUID parentId,
		String title,
		Integer orderNo,
		WbsStatus status,
		Instant createdAt,
		Instant updatedAt
) {
	public static WbsItemResponse from(WbsItemResult result) {
		return new WbsItemResponse(
				result.id(),
				result.roomId(),
				result.parentId(),
				result.title(),
				result.orderNo(),
				result.status(),
				result.createdAt(),
				result.updatedAt()
		);
	}
}
