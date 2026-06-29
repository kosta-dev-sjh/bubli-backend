package com.bubli.project.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.repository.RoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectRoomAccessPublicService {

    private final RoomMemberRepository roomMemberRepository;

    @Transactional(readOnly = true)
    public void requireRoomMember(UUID roomId, UUID userId) {
        if (roomId == null || userId == null || !roomMemberRepository.existsActiveMember(roomId, userId)) {
            throw new BusinessException(ErrorCode.PROJECT_403_001);
        }
    }
}
