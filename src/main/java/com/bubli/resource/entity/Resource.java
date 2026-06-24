package com.bubli.resource.entity;

import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resources")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resource {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(nullable = false, length = 200)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceKind kind;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceVisibility visibility;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceStatus status;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	private Resource(
			UUID ownerId,
			UUID roomId,
			String title,
			ResourceKind kind,
			ResourceVisibility visibility,
			ResourceStatus status
	) {
		this.ownerId = ownerId;
		this.roomId = roomId;
		this.title = title;
		this.kind = kind;
		this.visibility = visibility;
		this.status = status;
	}

	public static Resource create(
			UUID ownerId,
			UUID roomId,
			String title,
			ResourceKind kind,
			ResourceVisibility visibility,
			ResourceStatus status
	) {
		return new Resource(ownerId, roomId, title, kind, visibility, status);
	}

	public void updateTitle(String title) {
		if (title != null) {
			this.title = title;
		}
	}

	public void markDeleted(Instant deletedAt) {
		this.deletedAt = deletedAt;
		this.status = ResourceStatus.DELETED;
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
