package com.bubli.work.wbs.service;

import com.bubli.work.wbs.dto.CreateWbsItemCommand;
import com.bubli.work.wbs.dto.WbsItemResult;

import java.util.List;
import java.util.UUID;

public interface WbsItemPublicService {

	void assertRoomWbsItem(UUID roomId, UUID wbsItemId);

	List<WbsItemResult> getRoomItemsForBoard(UUID roomId);

	List<WbsItemResult> getRoomContextItems(UUID roomId, int limit);

	WbsItemResult create(UUID userId, UUID roomId, CreateWbsItemCommand command);
}
