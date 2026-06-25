package com.bubli.work.task.entity;

import com.bubli.work.task.type.TaskStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "owner_user_id")
	private UUID ownerUserId;

	@Column(name = "assignee_user_id")
	private UUID assigneeUserId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "wbs_item_id")
	private UUID wbsItemId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(columnDefinition = "text")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TaskStatus status = TaskStatus.TODO;

	@Column(name = "due_at")
	private Instant dueAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static Task createPersonal(UUID ownerUserId, String title, String description,
			TaskStatus status, Instant dueAt) {
		Task task = new Task();
		task.ownerUserId = ownerUserId;
		task.title = title;
		task.description = description;
		task.status = status == null ? TaskStatus.TODO : status;
		task.dueAt = dueAt;
		return task;
	}

	public static Task createRoomTask(UUID roomId, UUID assigneeUserId, UUID wbsItemId,
			String title, String description, TaskStatus status, Instant dueAt) {
		Task task = new Task();
		task.roomId = roomId;
		task.assigneeUserId = assigneeUserId;
		task.wbsItemId = wbsItemId;
		task.title = title;
		task.description = description;
		task.status = status == null ? TaskStatus.TODO : status;
		task.dueAt = dueAt;
		return task;
	}

	public void update(String title, String description, UUID assigneeUserId,
			UUID wbsItemId, TaskStatus status, Instant dueAt) {
		if (title != null) {
			this.title = title;
		}
		if (description != null) {
			this.description = description;
		}
		if (assigneeUserId != null) {
			this.assigneeUserId = assigneeUserId;
		}
		if (wbsItemId != null) {
			this.wbsItemId = wbsItemId;
		}
		if (status != null) {
			this.status = status;
		}
		if (dueAt != null) {
			this.dueAt = dueAt;
		}
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
