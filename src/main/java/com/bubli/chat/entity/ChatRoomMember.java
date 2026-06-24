package com.bubli.chat.entity;

import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.global.entity.BaseTimeEntity;
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
public class ChatRoomMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "chat_room_id", nullable = false)
	private UUID chatRoomId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "last_read_message_id")
	private UUID lastReadMessageId;

	@Column(name = "last_read_at")
	private Instant lastReadAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ChatMemberStatus status = ChatMemberStatus.ACTIVE;

}
