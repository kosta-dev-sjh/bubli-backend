package com.bubli.chat.service;

import java.util.List;
import java.util.UUID;

public interface ChatRoomAccessPublicService {

    void assertActiveMember(UUID userId, UUID chatRoomId);

    List<UUID> findActiveMemberIds(UUID chatRoomId);
}
