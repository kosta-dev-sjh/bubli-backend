package com.bubli.user.entity;

import com.bubli.global.entity.CreatedAtEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "friendships",
	uniqueConstraints = @UniqueConstraint(name = "uk_friendships_pair", columnNames = {"user_id", "friend_user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship extends CreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "friend_user_id", nullable = false)
	private UUID friendUserId;

	@Column(name = "accepted_at")
	private Instant acceptedAt;

}
