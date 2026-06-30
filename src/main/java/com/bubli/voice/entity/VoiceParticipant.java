package com.bubli.voice.entity;

import com.bubli.voice.type.VoiceParticipantStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "voice_participants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceParticipant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "voice_room_id", nullable = false)
	private UUID voiceRoomId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VoiceParticipantStatus status;

	@Column(name = "joined_at", nullable = false)
	private Instant joinedAt;

	@Column(name = "left_at")
	private Instant leftAt;

	@Column(name = "mic_status", length = 30)
	private String micStatus;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

	public static VoiceParticipant join(UUID voiceRoomId, UUID userId) {
		VoiceParticipant p = new VoiceParticipant();
		p.voiceRoomId = voiceRoomId;
		p.userId = userId;
		p.status = VoiceParticipantStatus.JOINED;
		p.joinedAt = Instant.now();
		return p;
	}

	public void leave() {
		this.status = VoiceParticipantStatus.LEFT;
		this.leftAt = Instant.now();
	}

}
