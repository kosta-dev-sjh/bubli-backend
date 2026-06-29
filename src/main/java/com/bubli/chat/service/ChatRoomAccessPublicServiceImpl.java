package com.bubli.chat.service;

import com.bubli.chat.repository.ChatRoomMemberRepository;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatRoomAccessPublicServiceImpl implements ChatRoomAccessPublicService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public void assertActiveMember(UUID userId, UUID chatRoomId) {
        boolean activeMember = chatRoomMemberRepository.existsByChatRoomIdAndUserIdAndStatus(
                chatRoomId,
                userId,
                ChatMemberStatus.ACTIVE
        );
        if (!activeMember) {
            throw new BusinessException(ErrorCode.CHAT_403_001);
        }
    }
}
