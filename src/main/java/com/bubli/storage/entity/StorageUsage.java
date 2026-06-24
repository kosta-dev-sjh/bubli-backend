package com.bubli.storage.entity;

import com.bubli.storage.type.StorageScope;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "storage_usage",
	uniqueConstraints = @UniqueConstraint(name = "uk_storage_usage_scope", columnNames = {"user_id", "room_id", "storage_scope"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageUsage {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id")
	private UUID userId;

	@Column(name = "room_id")
	private UUID roomId;

	@Enumerated(EnumType.STRING)
	@Column(name = "storage_scope", nullable = false, length = 30)
	private StorageScope storageScope;

	@Column(name = "used_bytes", nullable = false)
	private Long usedBytes = 0L;

	@Column(name = "limit_bytes", nullable = false)
	private Long limitBytes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static StorageUsage create(
			UUID userId,
			UUID roomId,
			StorageScope storageScope,
			long usedBytes,
			long limitBytes
	) {
		StorageUsage storageUsage = new StorageUsage();
		storageUsage.userId = userId;
		storageUsage.roomId = roomId;
		storageUsage.storageScope = storageScope;
		storageUsage.usedBytes = usedBytes;
		storageUsage.limitBytes = limitBytes;
		return storageUsage;
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
