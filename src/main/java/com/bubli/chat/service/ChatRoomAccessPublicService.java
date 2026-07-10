package com.bubli.chat.service;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ChatRoomAccessPublicService {

    void assertActiveMember(UUID userId, UUID chatRoomId);

    List<UUID> findActiveMemberIds(UUID chatRoomId);

    boolean isDirectChatRoom(UUID chatRoomId);

    // 프로젝트룸에는 항상 연결된 소통 채팅방(ChatType.ROOM)이 있다 — 멤버 목록이 이 채팅방
    // 멤버십과 동기화되어 있으므로(RoomChatPublicService.addMember/removeMember), 룸 보이스
    // 알림 대상을 구할 때 재사용한다.
    Optional<UUID> findRoomChatRoomId(UUID roomId);
}
