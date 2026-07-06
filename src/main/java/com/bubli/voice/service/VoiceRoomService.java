package com.bubli.voice.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.chat.service.ChatRoomAccessPublicService;
import com.bubli.personal.notification.service.NotificationPublicService;
import com.bubli.personal.notification.type.NotificationSourceType;
import com.bubli.project.service.ProjectRoomAccessPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import com.bubli.user.dto.UserResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.voice.config.LiveKitProperties;
import com.bubli.voice.dto.VoiceParticipantResponse;
import com.bubli.voice.dto.VoiceRoomResponse;
import com.bubli.voice.dto.VoiceTokenResponse;
import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.repository.VoiceParticipantRepository;
import com.bubli.voice.repository.VoiceRoomRepository;
import com.bubli.voice.type.VoiceParticipantStatus;
import com.bubli.voice.type.VoiceRoomStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceRoomService {

    private static final String DEFAULT_MIC_STATUS = "UNMUTED";

    private final VoiceRoomRepository voiceRoomRepository;
    private final VoiceParticipantRepository voiceParticipantRepository;
    private final ProjectRoomAccessPublicService projectRoomAccessPublicService;
    private final ProjectRoomEventPublicService projectRoomEventPublicService;
    private final ChatRoomAccessPublicService chatRoomAccessPublicService;
    private final UserPublicService userPublicService;
    private final NotificationPublicService notificationPublicService;
    private final LiveKitProperties liveKitProperties;

    @Transactional
    public VoiceRoomResponse createVoiceRoom(UUID userId, UUID roomId, UUID chatRoomId) {
        if (chatRoomId != null) {
            return createChatVoiceRoom(userId, chatRoomId);
        }
        return createProjectVoiceRoom(userId, roomId);
    }

    private VoiceRoomResponse createProjectVoiceRoom(UUID userId, UUID roomId) {
        projectRoomAccessPublicService.requireRoomMember(roomId, userId);
        voiceRoomRepository.lockRoomOpenCreation(lockKey(roomId));

        Optional<VoiceRoom> existing = voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN);
        if (existing.isPresent()) {
            VoiceRoom room = existing.get();
            List<VoiceParticipant> participants = currentParticipants(room.getId());
            Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());
            return toRoomResponse(room, participants.stream()
                    .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                    .toList());
        }

        VoiceRoom voiceRoom = voiceRoomRepository.save(VoiceRoom.create(roomId, userId));
        VoiceParticipant participant = voiceParticipantRepository.save(VoiceParticipant.join(voiceRoom.getId(), userId));
        projectRoomEventPublicService.recordVoiceRoomCreated(userId, roomId, voiceRoom.getId(), voiceRoom.getLivekitRoomName());
        projectRoomEventPublicService.recordVoiceParticipantJoined(userId, roomId, voiceRoom.getId(), participant.getId(), participant.getUserId());

        UserResult user = userPublicService.getUser(userId);
        return toRoomResponse(voiceRoom, List.of(toParticipantResponse(participant, user.name())));
    }

    private VoiceRoomResponse createChatVoiceRoom(UUID userId, UUID chatRoomId) {
        chatRoomAccessPublicService.assertActiveMember(userId, chatRoomId);

        Optional<VoiceRoom> existing = voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN);
        if (existing.isPresent()) {
            VoiceRoom room = existing.get();
            List<VoiceParticipant> participants = voiceParticipantRepository.findByVoiceRoomId(room.getId());
            Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());
            return toRoomResponse(room, participants.stream()
                    .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                    .toList());
        }

        VoiceRoom voiceRoom = voiceRoomRepository.save(VoiceRoom.createForChatRoom(chatRoomId, userId));
        VoiceParticipant participant = voiceParticipantRepository.save(VoiceParticipant.join(voiceRoom.getId(), userId));

        UserResult user = userPublicService.getUser(userId);
        notifyChatVoiceCallStarted(userId, user.name(), chatRoomId);
        return toRoomResponse(voiceRoom, List.of(toParticipantResponse(participant, user.name())));
    }

    private void notifyChatVoiceCallStarted(UUID callerUserId, String callerName, UUID chatRoomId) {
        chatRoomAccessPublicService.findActiveMemberIds(chatRoomId).stream()
                .filter(memberId -> !memberId.equals(callerUserId))
                .forEach(memberId -> notificationPublicService.create(
                        memberId,
                        NotificationSourceType.VOICE_CALL,
                        chatRoomId,
                        callerName,
                        "보이스 통화를 시작했습니다"
                ));
    }

    private String lockKey(UUID roomId) {
        return "voice-room-open:" + roomId;
    }

    @Transactional(readOnly = true)
    public VoiceRoomResponse getVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        requireRoomMemberIfRoomVoice(userId, voiceRoom);
        List<VoiceParticipant> participants = currentParticipants(voiceRoomId);

        Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());

        List<VoiceParticipantResponse> participantResponses = participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList();

        return toRoomResponse(voiceRoom, participantResponses);
    }

    @Transactional(readOnly = true)
    public VoiceRoomResponse getOpenVoiceRoomByProjectRoom(UUID userId, UUID roomId) {
        projectRoomAccessPublicService.requireRoomMember(roomId, userId);

        VoiceRoom voiceRoom = voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_404_001));
        List<VoiceParticipant> participants = currentParticipants(voiceRoom.getId());
        Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());

        return toRoomResponse(voiceRoom, participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList());
    }

    @Transactional
    public VoiceTokenResponse issueToken(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        if (voiceRoom.getStatus() == VoiceRoomStatus.ENDED) {
            throw new BusinessException(ErrorCode.VOICE_409_001);
        }
        if (voiceRoom.getRoomId() != null) {
            projectRoomAccessPublicService.requireRoomMember(voiceRoom.getRoomId(), userId);
        } else if (voiceRoom.getChatRoomId() != null) {
            chatRoomAccessPublicService.assertActiveMember(userId, voiceRoom.getChatRoomId());
        }

        boolean[] joined = {false};
        VoiceParticipant participant = voiceParticipantRepository
                .findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoomId, userId)
                .orElseGet(() -> {
                    joined[0] = true;
                    return voiceParticipantRepository.save(VoiceParticipant.join(voiceRoomId, userId));
                });
        if (participant.getStatus() != VoiceParticipantStatus.JOINED) {
            joined[0] = true;
            participant.rejoin();
        }
        if (joined[0] && voiceRoom.getRoomId() != null) {
            projectRoomEventPublicService.recordVoiceParticipantJoined(
                    userId,
                    voiceRoom.getRoomId(),
                    voiceRoom.getId(),
                    participant.getId(),
                    participant.getUserId()
            );
        }

        Instant expiresAt = Instant.now().plusSeconds(3600);
        String token = generateLiveKitToken(userId, voiceRoom.getLivekitRoomName(), expiresAt);

        return new VoiceTokenResponse(
                liveKitProperties.serverUrl(),
                token,
                voiceRoomId,
                participant.getId(),
                expiresAt
        );
    }

    @Transactional
    public VoiceParticipantResponse updateMicStatus(UUID userId, UUID voiceRoomId, String micStatus) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        requireRoomMemberIfRoomVoice(userId, voiceRoom);
        VoiceParticipant participant = latestParticipant(voiceRoomId, userId)
                .filter(p -> p.getStatus() == VoiceParticipantStatus.JOINED)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_404_001));
        participant.updateMicStatus(micStatus);
        if (voiceRoom.getRoomId() != null) {
            projectRoomEventPublicService.recordVoiceParticipantMicUpdated(
                    userId,
                    voiceRoom.getRoomId(),
                    voiceRoom.getId(),
                    participant.getId(),
                    participant.getUserId(),
                    micStatus
            );
        }
        UserResult user = userPublicService.getUser(userId);
        return toParticipantResponse(participant, user.name());
    }

    @Transactional
    public VoiceRoomResponse leaveVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        requireRoomMemberIfRoomVoice(userId, voiceRoom);
        latestParticipant(voiceRoomId, userId).ifPresent(participant -> {
            if (participant.getStatus() == VoiceParticipantStatus.JOINED) {
                participant.leave();
                if (voiceRoom.getRoomId() != null) {
                    projectRoomEventPublicService.recordVoiceParticipantLeft(
                            userId,
                            voiceRoom.getRoomId(),
                            voiceRoom.getId(),
                            participant.getId(),
                            participant.getUserId()
                    );
                }
            }
        });

        List<VoiceParticipant> participants = voiceParticipantRepository.findByVoiceRoomId(voiceRoomId);
        Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());
        return toRoomResponse(voiceRoom, participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList());
    }

    @Transactional
    public VoiceRoomResponse endVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        if (!userId.equals(voiceRoom.getCreatedByUserId())) {
            throw new BusinessException(ErrorCode.VOICE_403_001);
        }

        voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoomId, VoiceParticipantStatus.JOINED)
                .forEach(VoiceParticipant::leave);

        voiceRoom.end();
        if (voiceRoom.getRoomId() != null) {
            projectRoomEventPublicService.recordVoiceRoomEnded(userId, voiceRoom.getRoomId(), voiceRoom.getId());
        }

        List<VoiceParticipant> participants = voiceParticipantRepository.findByVoiceRoomId(voiceRoomId);
        Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());
        return toRoomResponse(voiceRoom, participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList());
    }

    private VoiceRoom findRoom(UUID voiceRoomId) {
        return voiceRoomRepository.findById(voiceRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_404_001));
    }

    private Optional<VoiceParticipant> latestParticipant(UUID voiceRoomId, UUID userId) {
        return voiceParticipantRepository.findFirstByVoiceRoomIdAndUserIdOrderByCreatedAtDesc(voiceRoomId, userId);
    }

    private List<VoiceParticipant> currentParticipants(UUID voiceRoomId) {
        return voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoomId, VoiceParticipantStatus.JOINED);
    }

    private void requireRoomMemberIfRoomVoice(UUID userId, VoiceRoom voiceRoom) {
        if (voiceRoom.getRoomId() != null) {
            projectRoomAccessPublicService.requireRoomMember(voiceRoom.getRoomId(), userId);
        }
    }

    private Map<UUID, String> fetchUserNames(List<UUID> userIds) {
        List<UUID> distinctUserIds = userIds.stream()
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            return Map.of();
        }
        try {
            return userPublicService.getUsers(distinctUserIds).values().stream()
                .collect(Collectors.toMap(
                        UserResult::id,
                        user -> Optional.ofNullable(user.name()).orElse("")
                ));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private String generateLiveKitToken(UUID userId, String roomName, Instant expiresAt) {
        Map<String, Object> videoGrants = Map.of(
                "room", roomName,
                "roomJoin", true,
                "canPublish", true,
                "canSubscribe", true
        );

        SecretKey key = Keys.hmacShaKeyFor(liveKitProperties.apiSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .issuer(liveKitProperties.apiKey())
                .subject(userId.toString())
                .expiration(Date.from(expiresAt))
                .issuedAt(new Date())
                .claim("video", videoGrants)
                .signWith(key)
                .compact();
    }

    private VoiceRoomResponse toRoomResponse(VoiceRoom room, List<VoiceParticipantResponse> participants) {
        return new VoiceRoomResponse(
                room.getId(),
                room.getRoomId(),
                room.getChatRoomId(),
                room.getLivekitRoomName(),
                room.getStatus().name(),
                participants,
                room.getCreatedAt(),
                room.getCreatedByUserId()
        );
    }

    private VoiceParticipantResponse toParticipantResponse(VoiceParticipant p, String userName) {
        return new VoiceParticipantResponse(
                p.getId(),
                p.getUserId(),
                userName,
                p.getStatus().name(),
                p.getJoinedAt(),
                p.getLeftAt(),
                Optional.ofNullable(p.getMicStatus()).orElse(DEFAULT_MIC_STATUS)
        );
    }
}
