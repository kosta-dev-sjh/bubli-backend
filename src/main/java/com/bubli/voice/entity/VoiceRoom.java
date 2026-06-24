package com.bubli.voice.entity;

import com.bubli.global.entity.CreatedAtEntity;
import com.bubli.voice.type.VoiceRoomStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "voice_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VoiceRoom extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "chat_room_id", nullable = false)
	private UUID chatRoomId;

	@Column(name = "livekit_room_name", nullable = false, unique = true, length = 120)
	private String livekitRoomName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private VoiceRoomStatus status = VoiceRoomStatus.OPEN;

}
