package com.bubli.storage.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.storage.type.StorageScope;

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
public class StorageUsage extends BaseTimeEntity {

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

}
