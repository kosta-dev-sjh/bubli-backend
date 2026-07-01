package com.bubli.chat.entity;

import com.bubli.chat.type.ChatRoomStatus;
import com.bubli.chat.type.ChatType;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id")
	private UUID roomId;

	@Enumerated(EnumType.STRING)
	@Column(name = "chat_type", nullable = false, length = 30)
	private ChatType chatType;

	@Column(length = 120)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ChatRoomStatus status = ChatRoomStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static ChatRoom createRoom(UUID roomId, String name) {
		ChatRoom chatRoom = new ChatRoom();
		chatRoom.roomId = roomId;
		chatRoom.chatType = ChatType.ROOM;
		chatRoom.name = name;
		chatRoom.status = ChatRoomStatus.ACTIVE;
		return chatRoom;
	}

	public static ChatRoom createDirect(String name) {
		ChatRoom chatRoom = new ChatRoom();
		chatRoom.chatType = ChatType.DIRECT;
		chatRoom.name = name;
		chatRoom.status = ChatRoomStatus.ACTIVE;
		return chatRoom;
	}

	public static ChatRoom createGroup(String name) {
		ChatRoom chatRoom = new ChatRoom();
		chatRoom.chatType = ChatType.GROUP;
		chatRoom.name = name;
		chatRoom.status = ChatRoomStatus.ACTIVE;
		return chatRoom;
	}

	@PrePersist
	private void onCreate() {
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	private void onUpdate() {
		this.updatedAt = Instant.now();
	}

}
