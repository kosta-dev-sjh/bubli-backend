package com.bubli.widget.entity;

import com.bubli.global.entity.BaseTimeEntity;
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
public class WidgetDailySummary extends BaseTimeEntity {

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

}
