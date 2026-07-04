package com.bubli.user.service;

import java.util.UUID;

public interface FriendshipPublicService {

	void assertAcceptedFriend(UUID userId, UUID friendUserId);
}
