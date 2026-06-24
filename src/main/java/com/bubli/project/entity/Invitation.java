package com.bubli.project.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.project.type.InvitationStatus;
import com.bubli.project.type.RoomMemberRole;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "invitations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "inviter_user_id", nullable = false)
	private UUID inviterUserId;

	@Column(name = "invitee_user_id", nullable = false)
	private UUID inviteeUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RoomMemberRole role = RoomMemberRole.MEMBER;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private InvitationStatus status = InvitationStatus.PENDING;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

}
