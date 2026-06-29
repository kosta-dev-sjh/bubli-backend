package com.bubli.project.entity;

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
public class Invitation {

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

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static Invitation create(UUID roomId, UUID inviterUserId, UUID inviteeUserId,
			RoomMemberRole role, Instant expiresAt) {
		Invitation invitation = new Invitation();
		invitation.roomId = roomId;
		invitation.inviterUserId = inviterUserId;
		invitation.inviteeUserId = inviteeUserId;
		invitation.role = role == null ? RoomMemberRole.MEMBER : role;
		invitation.status = InvitationStatus.PENDING;
		invitation.expiresAt = expiresAt;
		return invitation;
	}

	public boolean isPending() {
		return status == InvitationStatus.PENDING;
	}

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now) || expiresAt.equals(now);
	}

	public void accept(Instant acceptedAt) {
		this.status = InvitationStatus.ACCEPTED;
		this.acceptedAt = acceptedAt;
	}

	public void expire() {
		this.status = InvitationStatus.EXPIRED;
	}

	public void cancel() {
		this.status = InvitationStatus.CANCELED;
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
