package com.bubli.work.wbs.dto;

import com.bubli.work.wbs.type.WbsStatus;

import java.util.UUID;

public record UpdateWbsItemCommand(
		UUID parentId,
		String title,
		Integer orderNo,
		WbsStatus status
) {
}
