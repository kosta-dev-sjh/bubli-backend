package com.bubli.voice.repository;

import com.bubli.voice.entity.VoiceRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoiceRoomRepository extends JpaRepository<VoiceRoom, UUID> {
}
