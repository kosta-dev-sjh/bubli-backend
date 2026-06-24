package com.bubli.resource.entity;


import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_versions",
	uniqueConstraints = @UniqueConstraint(name = "uk_resource_versions_resource_version", columnNames = {"resource_id", "version_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "version_no", nullable = false)
	private Integer versionNo;

	@Column(name = "file_id", nullable = false)
	private UUID fileId;

	@Column(name = "created_by", nullable = false)
	private UUID createdBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
