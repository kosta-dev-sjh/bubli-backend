package com.bubli.voice.repository;

import com.bubli.voice.entity.VoiceRoom;
import com.bubli.voice.type.VoiceRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoom, UUID> {

    Optional<VoiceRoom> findByRoomIdAndStatus(UUID roomId, VoiceRoomStatus status);

    Optional<VoiceRoom> findByLivekitRoomName(String livekitRoomName);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    void lockRoomOpenCreation(@Param("lockKey") String lockKey);
}
