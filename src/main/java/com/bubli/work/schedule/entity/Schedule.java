package com.bubli.work.schedule.entity;

import com.bubli.work.schedule.type.ScheduleSyncStatus;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(name = "task_id")
	private UUID taskId;

	@Column(name = "wbs_item_id")
	private UUID wbsItemId;

	@Column(name = "google_event_id", unique = true, length = 255)
	private String googleEventId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at")
	private Instant endsAt;

	@Column(name = "is_all_day", nullable = false)
	private boolean allDay;

	@Enumerated(EnumType.STRING)
	@Column(name = "sync_status", nullable = false, length = 30)
	private ScheduleSyncStatus syncStatus = ScheduleSyncStatus.LOCAL_ONLY;

	@Column(name = "last_synced_at")
	private Instant lastSyncedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static Schedule create(UUID ownerUserId, UUID roomId, UUID taskId, UUID wbsItemId,
			String title, Instant startsAt, Instant endsAt, boolean allDay) {
		Schedule schedule = new Schedule();
		schedule.ownerUserId = ownerUserId;
		schedule.roomId = roomId;
		schedule.taskId = taskId;
		schedule.wbsItemId = wbsItemId;
		schedule.title = title;
		schedule.startsAt = startsAt;
		schedule.endsAt = endsAt;
		schedule.allDay = allDay;
		schedule.syncStatus = ScheduleSyncStatus.LOCAL_ONLY;
		return schedule;
	}

	public void update(String title, Instant startsAt, Instant endsAt, Boolean allDay,
			UUID taskId, UUID wbsItemId) {
		if (title != null) {
			this.title = title;
		}
		if (startsAt != null) {
			this.startsAt = startsAt;
		}
		this.endsAt = endsAt;
		if (allDay != null) {
			this.allDay = allDay;
		}
		this.taskId = taskId;
		this.wbsItemId = wbsItemId;
		this.syncStatus = ScheduleSyncStatus.LOCAL_ONLY;
	}

	public void markSynced(String googleEventId) {
		this.googleEventId = googleEventId;
		this.syncStatus = ScheduleSyncStatus.SYNCED;
		this.lastSyncedAt = Instant.now();
	}

	public void markSyncFailed() {
		this.syncStatus = ScheduleSyncStatus.SYNC_FAILED;
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
