package com.bubli.chat.service;

import java.util.UUID;

public interface ChatRoomAccessPublicService {

    void assertActiveMember(UUID userId, UUID chatRoomId);
}
