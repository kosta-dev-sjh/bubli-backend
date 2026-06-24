package com.bubli.project.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "room_members",
	uniqueConstraints = @UniqueConstraint(name = "uk_room_members_room_user", columnNames = {"room_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RoomMemberRole role = RoomMemberRole.MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RoomMemberStatus status = RoomMemberStatus.ACTIVE;

}
