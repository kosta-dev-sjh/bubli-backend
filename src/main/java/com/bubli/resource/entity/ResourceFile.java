package com.bubli.resource.entity;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceFile {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "storage_key", nullable = false, unique = true, length = 500)
	private String storageKey;

	@Column(name = "original_name", nullable = false, length = 255)
	private String originalName;

	@Column(name = "mime_type", nullable = false, length = 120)
	private String mimeType;

	@Column(name = "size_bytes", nullable = false)
	private Long sizeBytes;

	@Column(length = 128)
	private String checksum;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public static ResourceFile create(UUID resourceId, String storageKey, String originalName,
			String mimeType, Long sizeBytes, String checksum) {
		ResourceFile file = new ResourceFile();
		file.resourceId = resourceId;
		file.storageKey = storageKey;
		file.originalName = originalName;
		file.mimeType = mimeType;
		file.sizeBytes = sizeBytes;
		file.checksum = checksum;
		return file;
	}

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
