package com.bubli.chat.service;

import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomChatPublicServiceImpl implements RoomChatPublicService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    @Transactional
    public void createRoomChat(UUID roomId, String roomName, UUID creatorUserId) {
        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createRoom(roomId, roomName));
        chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), creatorUserId));
    }

    @Override
    @Transactional
    public void addMember(UUID roomId, UUID userId) {
        chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM).ifPresent(chatRoom ->
                chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId)
                        .ifPresentOrElse(
                                ChatRoomMember::reactivate,
                                () -> chatRoomMemberRepository.save(ChatRoomMember.create(chatRoom.getId(), userId))
                        )
        );
    }

    @Override
    @Transactional
    public void removeMember(UUID roomId, UUID userId) {
        chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM).ifPresent(chatRoom ->
                chatRoomMemberRepository.findByChatRoomIdAndUserIdAndStatus(
                        chatRoom.getId(), userId, ChatMemberStatus.ACTIVE
                ).ifPresent(ChatRoomMember::leave)
        );
    }
}
