package com.bubli.chat.entity;

import com.bubli.chat.type.MessageType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_messages",
	uniqueConstraints = @UniqueConstraint(name = "uk_chat_messages_room_sequence", columnNames = {"chat_room_id", "room_sequence"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "chat_room_id", nullable = false)
	private UUID chatRoomId;

	@Column(name = "sender_user_id", nullable = false)
	private UUID senderUserId;

	@Column(name = "client_message_id", nullable = false, unique = true, length = 120)
	private String clientMessageId;

	@Column(name = "room_sequence", nullable = false)
	private Long roomSequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false, length = 30)
	private MessageType messageType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String body;

	@Column(name = "resource_id")
	private UUID resourceId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
