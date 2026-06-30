package com.bubli.voice.repository;

import com.bubli.voice.entity.VoiceParticipant;
import com.bubli.voice.type.VoiceParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoiceParticipantRepository extends JpaRepository<VoiceParticipant, UUID> {

    List<VoiceParticipant> findByVoiceRoomId(UUID voiceRoomId);

    Optional<VoiceParticipant> findByVoiceRoomIdAndUserId(UUID voiceRoomId, UUID userId);

    List<VoiceParticipant> findByVoiceRoomIdAndStatus(UUID voiceRoomId, VoiceParticipantStatus status);
}
