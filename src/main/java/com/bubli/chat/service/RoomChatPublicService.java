package com.bubli.chat.service;

import java.util.UUID;

public interface RoomChatPublicService {

    void createRoomChat(UUID roomId, String roomName, UUID creatorUserId);

    void addMember(UUID roomId, UUID userId);

    void removeMember(UUID roomId, UUID userId);
}
