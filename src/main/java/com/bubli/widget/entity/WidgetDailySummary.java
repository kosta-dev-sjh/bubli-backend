package com.bubli.widget.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "widget_daily_summaries",
	uniqueConstraints = @UniqueConstraint(name = "uk_widget_daily_summaries_rollup", columnNames = {"user_id", "device_id", "summary_date", "bubble_setting_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetDailySummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "device_id", nullable = false, length = 120)
	private String deviceId;

	@Column(name = "rollup_key", nullable = false, unique = true, length = 160)
	private String rollupKey;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Column(name = "bubble_setting_id", nullable = false)
	private UUID bubbleSettingId;

	@Column(name = "open_count", nullable = false)
	private Integer openCount = 0;

	@Column(name = "interaction_count", nullable = false)
	private Integer interactionCount = 0;

	@Column(name = "visible_seconds", nullable = false)
	private Long visibleSeconds = 0L;

	@Column(name = "synced_at", nullable = false)
	private Instant syncedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

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

	public static WidgetDailySummary create(UUID userId, String deviceId, String rollupKey,
			LocalDate summaryDate, UUID bubbleSettingId,
			int openCount, int interactionCount, long visibleSeconds, Instant syncedAt) {
		WidgetDailySummary s = new WidgetDailySummary();
		s.userId = userId;
		s.deviceId = deviceId;
		s.rollupKey = rollupKey;
		s.summaryDate = summaryDate;
		s.bubbleSettingId = bubbleSettingId;
		s.openCount = openCount;
		s.interactionCount = interactionCount;
		s.visibleSeconds = visibleSeconds;
		s.syncedAt = syncedAt;
		return s;
	}

}
