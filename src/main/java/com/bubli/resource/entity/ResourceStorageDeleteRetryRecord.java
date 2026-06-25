package com.bubli.resource.entity;

import com.bubli.resource.type.ResourceStorageDeleteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_storage_delete_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceStorageDeleteRetryRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "file_id")
	private UUID fileId;

	@Column(name = "storage_key", nullable = false, length = 500)
	private String storageKey;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceStorageDeleteStatus status;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	@Column(name = "last_error_message", length = 1000)
	private String lastErrorMessage;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static ResourceStorageDeleteRetryRecord create(
			UUID resourceId,
			UUID fileId,
			String storageKey,
			String lastErrorMessage
	) {
		ResourceStorageDeleteRetryRecord record = new ResourceStorageDeleteRetryRecord();
		record.resourceId = resourceId;
		record.fileId = fileId;
		record.storageKey = storageKey;
		record.status = ResourceStorageDeleteStatus.PENDING;
		record.retryCount = 0;
		record.lastErrorMessage = lastErrorMessage;
		return record;
	}

	public void markDeleted() {
		this.status = ResourceStorageDeleteStatus.DELETED;
		this.lastErrorMessage = null;
	}

	public void markFailed(String errorMessage) {
		this.status = ResourceStorageDeleteStatus.FAILED;
		this.retryCount++;
		this.lastErrorMessage = errorMessage;
	}

	public void markDeadLetter(String errorMessage) {
		this.status = ResourceStorageDeleteStatus.DEAD_LETTER;
		this.lastErrorMessage = errorMessage;
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
