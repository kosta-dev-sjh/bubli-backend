package com.bubli.chat.entity;

import com.bubli.chat.type.ChatRoomStatus;
import com.bubli.chat.type.ChatType;
import com.bubli.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {

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

}
