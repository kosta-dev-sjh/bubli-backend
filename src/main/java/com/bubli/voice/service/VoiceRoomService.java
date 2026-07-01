package com.bubli.voice.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectRoomAccessPublicService;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceRoomService {

    private final VoiceRoomRepository voiceRoomRepository;
    private final VoiceParticipantRepository voiceParticipantRepository;
    private final ProjectRoomAccessPublicService projectRoomAccessPublicService;
    private final UserPublicService userPublicService;
    private final LiveKitProperties liveKitProperties;

    @Transactional
    public VoiceRoomResponse createVoiceRoom(UUID userId, UUID roomId) {
        projectRoomAccessPublicService.requireRoomMember(roomId, userId);

        voiceRoomRepository.findByRoomIdAndStatus(roomId, VoiceRoomStatus.OPEN).ifPresent(r -> {
            throw new BusinessException(ErrorCode.VOICE_409_002);
        });

        VoiceRoom voiceRoom = voiceRoomRepository.save(VoiceRoom.create(roomId, userId));
        VoiceParticipant participant = voiceParticipantRepository.save(VoiceParticipant.join(voiceRoom.getId(), userId));

        UserResult user = userPublicService.getUser(userId);
        List<VoiceParticipantResponse> participants = List.of(toParticipantResponse(participant, user.name()));
        return toRoomResponse(voiceRoom, participants);
    }

    @Transactional(readOnly = true)
    public VoiceRoomResponse getVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        List<VoiceParticipant> participants = voiceParticipantRepository.findByVoiceRoomId(voiceRoomId);

        List<UUID> userIds = participants.stream().map(VoiceParticipant::getUserId).toList();
        Map<UUID, String> nameMap = userPublicService.getUser(userId) != null
                ? fetchUserNames(userIds)
                : Map.of();

        List<VoiceParticipantResponse> participantResponses = participants.stream()
                .map(p -> toParticipantResponse(p, nameMap.getOrDefault(p.getUserId(), "")))
                .toList();

        return toRoomResponse(voiceRoom, participantResponses);
    }

    @Transactional
    public VoiceTokenResponse issueToken(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        if (voiceRoom.getStatus() == VoiceRoomStatus.ENDED) {
            throw new BusinessException(ErrorCode.VOICE_409_001);
        }
        if (voiceRoom.getRoomId() != null) {
            projectRoomAccessPublicService.requireRoomMember(voiceRoom.getRoomId(), userId);
        }

        VoiceParticipant participant = voiceParticipantRepository
                .findByVoiceRoomIdAndUserId(voiceRoomId, userId)
                .orElseGet(() -> voiceParticipantRepository.save(VoiceParticipant.join(voiceRoomId, userId)));

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
        findRoom(voiceRoomId);
        VoiceParticipant participant = voiceParticipantRepository.findByVoiceRoomIdAndUserId(voiceRoomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VOICE_404_001));
        participant.updateMicStatus(micStatus);
        UserResult user = userPublicService.getUser(userId);
        return toParticipantResponse(participant, user.name());
    }

    @Transactional
    public VoiceRoomResponse leaveVoiceRoom(UUID userId, UUID voiceRoomId) {
        VoiceRoom voiceRoom = findRoom(voiceRoomId);
        voiceParticipantRepository.findByVoiceRoomIdAndUserId(voiceRoomId, userId)
                .ifPresent(VoiceParticipant::leave);

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

    private Map<UUID, String> fetchUserNames(List<UUID> userIds) {
        return userIds.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> {
                            try {
                                return userPublicService.getUser(id).name();
                            } catch (Exception e) {
                                return "";
                            }
                        }
                ));
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
                room.getLivekitRoomName(),
                room.getStatus().name(),
                participants,
                room.getCreatedAt()
        );
    }

    private VoiceParticipantResponse toParticipantResponse(VoiceParticipant p, String userName) {
        return new VoiceParticipantResponse(
                p.getId(),
                p.getUserId(),
                userName,
                p.getStatus().name(),
                p.getJoinedAt(),
                p.getLeftAt()
        );
    }
}
