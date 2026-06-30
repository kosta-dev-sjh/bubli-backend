package com.bubli.voice.repository;

import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.type.VoiceRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoom, UUID> {

    Optional<VoiceRoom> findByRoomIdAndStatus(UUID roomId, VoiceRoomStatus status);

    Optional<VoiceRoom> findByLivekitRoomName(String livekitRoomName);
}
