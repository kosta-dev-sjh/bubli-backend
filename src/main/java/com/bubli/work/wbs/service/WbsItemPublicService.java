package com.bubli.work.wbs.service;

import java.util.UUID;

public interface WbsItemPublicService {

	void assertRoomWbsItem(UUID roomId, UUID wbsItemId);
}
