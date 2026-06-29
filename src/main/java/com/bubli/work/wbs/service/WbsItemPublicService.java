package com.bubli.work.wbs.service;

import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsItemResult;

import java.util.UUID;

public interface WbsItemPublicService {

	void assertRoomWbsItem(UUID roomId, UUID wbsItemId);

	WbsItemResult create(UUID userId, UUID roomId, CreateWbsItemCommand command);
}
