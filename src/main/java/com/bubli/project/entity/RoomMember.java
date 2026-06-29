package com.bubli.project.entity;

import com.bubli.project.type.RoomMemberRole;
import com.bubli.project.type.RoomMemberStatus;

import java.time.Instant;

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
public class RoomMember {

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static RoomMember createLeader(UUID roomId, UUID userId) {
		RoomMember roomMember = new RoomMember();
		roomMember.roomId = roomId;
		roomMember.userId = userId;
		roomMember.role = RoomMemberRole.PROJECT_LEADER;
		roomMember.status = RoomMemberStatus.ACTIVE;
		return roomMember;
	}

	public static RoomMember createMember(UUID roomId, UUID userId) {
		RoomMember roomMember = new RoomMember();
		roomMember.roomId = roomId;
		roomMember.userId = userId;
		roomMember.role = RoomMemberRole.MEMBER;
		roomMember.status = RoomMemberStatus.ACTIVE;
		return roomMember;
	}

	public static RoomMember create(UUID roomId, UUID userId, RoomMemberRole role) {
		RoomMember roomMember = new RoomMember();
		roomMember.roomId = roomId;
		roomMember.userId = userId;
		roomMember.role = role == null ? RoomMemberRole.MEMBER : role;
		roomMember.status = RoomMemberStatus.ACTIVE;
		return roomMember;
	}

	public void reactivate(RoomMemberRole role) {
		this.role = role == null ? RoomMemberRole.MEMBER : role;
		this.status = RoomMemberStatus.ACTIVE;
	}

	public void updateRole(RoomMemberRole role) {
		this.role = role == null ? RoomMemberRole.MEMBER : role;
	}

	public void leave() {
		this.status = RoomMemberStatus.LEFT;
	}

	public void remove() {
		this.status = RoomMemberStatus.REMOVED;
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
