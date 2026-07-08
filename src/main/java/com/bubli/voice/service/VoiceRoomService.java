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
import com.bubli.websocket.service.WebSocketPublishPublicService;
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
    private final WebSocketPublishPublicService webSocketPublishPublicService;

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
        String message = voiceScopeLabel(null, chatRoomId) + "를 시작했습니다";
        chatRoomAccessPublicService.findActiveMemberIds(chatRoomId).stream()
                .filter(memberId -> !memberId.equals(callerUserId))
                .forEach(memberId -> notificationPublicService.create(
                        memberId,
                        NotificationSourceType.VOICE_CALL,
                        chatRoomId,
                        callerName,
                        message
                ));
    }

    private String lockKey(UUID roomId) {
        return "voice-room-open:" + roomId;
    }

    // 알림 문구에 상황(1:1/그룹/프로젝트룸)을 붙여 어떤 통화인지 구분되게 한다 — 예전엔
    // "보이스 통화를 시작했습니다"/"전화를 취소했습니다"/"통화를 거절했습니다"처럼 표현이
    // 제각각이라 알림 목록만 봐서는 무슨 통화인지, 심지어 같은 종류의 알림인지도 알기 어려웠다.
    private String voiceScopeLabel(UUID roomId, UUID chatRoomId) {
        if (roomId != null) {
            return "룸 보이스";
        }
        return chatRoomAccessPublicService.isDirectChatRoom(chatRoomId) ? "1:1 보이스" : "그룹 보이스";
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

    @Transactional(readOnly = true)
    public VoiceRoomResponse getOpenVoiceRoomByChatRoom(UUID userId, UUID chatRoomId) {
        chatRoomAccessPublicService.assertActiveMember(userId, chatRoomId);

        VoiceRoom voiceRoom = voiceRoomRepository.findByChatRoomIdAndStatus(chatRoomId, VoiceRoomStatus.OPEN)
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

        if (joined[0]) {
            broadcastRoomState(voiceRoom);
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
        broadcastRoomState(voiceRoom);
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

        VoiceRoomResponse response = buildRoomResponse(voiceRoom);
        webSocketPublishPublicService.publishVoiceRoomEvent(response);
        return response;
    }

    @Transactional
    public void declineVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        if (voiceRoom.getChatRoomId() == null) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        if (voiceRoom.getStatus() != VoiceRoomStatus.OPEN) {
            return;
        }
        chatRoomAccessPublicService.assertActiveMember(userId, voiceRoom.getChatRoomId());

        UserResult decliner = userPublicService.getUser(userId);
        notificationPublicService.create(
                voiceRoom.getCreatedByUserId(),
                NotificationSourceType.VOICE_CALL_DECLINED,
                voiceRoom.getChatRoomId(),
                decliner.name(),
                voiceScopeLabel(null, voiceRoom.getChatRoomId()) + "를 거절했습니다"
        );
    }

    @Transactional
    public VoiceRoomResponse endVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        // 1:1 통화는 "나가기" 개념이 없다 — 둘 중 누구든 종료를 누르면 둘 다 끝나야 하므로
        // 개설자가 아니어도 종료를 허용한다. 그룹/프로젝트룸은 개설자만 전체 종료 가능(기존 정책 유지).
        boolean canEnd = userId.equals(voiceRoom.getCreatedByUserId())
                || (voiceRoom.getChatRoomId() != null && chatRoomAccessPublicService.isDirectChatRoom(voiceRoom.getChatRoomId()));
        if (!canEnd) {
            throw new BusinessException(ErrorCode.VOICE_403_001);
        }
        if (voiceRoom.getChatRoomId() != null) {
            chatRoomAccessPublicService.assertActiveMember(userId, voiceRoom.getChatRoomId());
        }

        // 발신자가 상대가 받기 전에 취소하는 경우 — 종료 처리 전에 판단해야 한다(leave() 이후엔
        // 참여자가 모두 LEFT라 판단 불가). 상대는 아직 참여 이력이 없으므로 이 시점에 JOINED가
        // 개설자 한 명뿐이면 "받기 전 취소"로 본다.
        boolean wasRingingBack = userId.equals(voiceRoom.getCreatedByUserId())
                && voiceRoom.getChatRoomId() != null
                && currentParticipants(voiceRoomId).size() <= 1;

        voiceParticipantRepository.findByVoiceRoomIdAndStatus(voiceRoomId, VoiceParticipantStatus.JOINED)
                .forEach(VoiceParticipant::leave);

        voiceRoom.end();
        if (voiceRoom.getRoomId() != null) {
            projectRoomEventPublicService.recordVoiceRoomEnded(userId, voiceRoom.getRoomId(), voiceRoom.getId());
        }

        if (wasRingingBack) {
            UserResult canceler = userPublicService.getUser(userId);
            String message = voiceScopeLabel(null, voiceRoom.getChatRoomId()) + "를 취소했습니다";
            chatRoomAccessPublicService.findActiveMemberIds(voiceRoom.getChatRoomId()).stream()
                    .filter(memberId -> !memberId.equals(userId))
                    .forEach(memberId -> notificationPublicService.create(
                            memberId,
                            NotificationSourceType.VOICE_CALL_CANCELED,
                            voiceRoom.getChatRoomId(),
                            canceler.name(),
                            message
                    ));
        }

        VoiceRoomResponse response = buildRoomResponse(voiceRoom);
        webSocketPublishPublicService.publishVoiceRoomEvent(response);
        return response;
    }

    private VoiceRoomResponse buildRoomResponse(VoiceRoom voiceRoom) {
        List<VoiceParticipant> participants = voiceParticipantRepository.findByVoiceRoomId(voiceRoom.getId());
        Map<UUID, String> nameMap = fetchUserNames(participants.stream().map(VoiceParticipant::getUserId).toList());
        return toRoomResponse(voiceRoom, participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList());
    }

    private void broadcastRoomState(VoiceRoom voiceRoom) {
        webSocketPublishPublicService.publishVoiceRoomEvent(buildRoomResponse(voiceRoom));
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
