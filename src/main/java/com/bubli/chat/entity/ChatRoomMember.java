package com.bubli.chat.entity;

import com.bubli.chat.type.ChatMemberStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_room_members",
	uniqueConstraints = @UniqueConstraint(name = "uk_chat_room_members_room_user", columnNames = {"chat_room_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "chat_room_id", nullable = false)
	private UUID chatRoomId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "last_read_message_id")
	private UUID lastReadMessageId;

	@Column(name = "last_read_sequence")
	private Long lastReadSequence;

	@Column(name = "last_read_at")
	private Instant lastReadAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ChatMemberStatus status = ChatMemberStatus.ACTIVE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static ChatRoomMember create(UUID chatRoomId, UUID userId) {
		ChatRoomMember member = new ChatRoomMember();
		member.chatRoomId = chatRoomId;
		member.userId = userId;
		member.status = ChatMemberStatus.ACTIVE;
		return member;
	}

	public void markRead(UUID lastReadMessageId, Long lastReadSequence, Instant lastReadAt) {
		this.lastReadMessageId = lastReadMessageId;
		this.lastReadSequence = lastReadSequence;
		this.lastReadAt = lastReadAt;
	}

	public void leave() {
		this.status = ChatMemberStatus.LEFT;
	}

	public void reactivate() {
		this.status = ChatMemberStatus.ACTIVE;
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
