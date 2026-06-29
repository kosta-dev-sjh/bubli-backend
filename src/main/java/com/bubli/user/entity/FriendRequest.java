package com.bubli.user.entity;

import com.bubli.user.type.FriendRequestStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "friend_requests",
	uniqueConstraints = @UniqueConstraint(name = "uk_friend_requests_pair", columnNames = {"requester_id", "receiver_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "requester_id", nullable = false)
	private UUID requesterId;

	@Column(name = "receiver_id", nullable = false)
	private UUID receiverId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private FriendRequestStatus status = FriendRequestStatus.PENDING;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static FriendRequest create(UUID requesterId, UUID receiverId) {
		FriendRequest req = new FriendRequest();
		req.requesterId = requesterId;
		req.receiverId = receiverId;
		req.status = FriendRequestStatus.PENDING;
		return req;
	}

	public void accept() {
		this.status = FriendRequestStatus.ACCEPTED;
		this.respondedAt = Instant.now();
	}

	public void reject() {
		this.status = FriendRequestStatus.REJECTED;
		this.respondedAt = Instant.now();
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
