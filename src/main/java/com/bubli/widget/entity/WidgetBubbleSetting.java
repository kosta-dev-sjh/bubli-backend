package com.bubli.widget.entity;

import com.bubli.widget.type.BubbleType;
import java.math.BigDecimal;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "widget_bubble_settings",
	uniqueConstraints = @UniqueConstraint(name = "uk_widget_bubble_settings_user_type", columnNames = {"user_id", "bubble_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetBubbleSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "bubble_type", nullable = false, length = 30)
	private BubbleType bubbleType;

	@Column(nullable = false)
	private boolean enabled;

	private Integer x;

	private Integer y;

	private Integer width;

	private Integer height;

	@Column(nullable = false)
	private boolean minimized;

	@Column(precision = 4, scale = 2)
	private BigDecimal opacity;

	@Column(name = "ghost_mode", nullable = false)
	private boolean ghostMode;

	@Column(name = "alert_enabled", nullable = false)
	private boolean alertEnabled = true;

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

	public static WidgetBubbleSetting create(UUID userId, BubbleType bubbleType) {
		WidgetBubbleSetting s = new WidgetBubbleSetting();
		s.userId = userId;
		s.bubbleType = bubbleType;
		s.enabled = true;
		s.minimized = false;
		s.ghostMode = false;
		s.alertEnabled = true;
		return s;
	}

	public void update(Boolean enabled, Integer x, Integer y, Integer width, Integer height,
			Boolean minimized, java.math.BigDecimal opacity, Boolean ghostMode, Boolean alertEnabled) {
		if (enabled != null) this.enabled = enabled;
		if (x != null) this.x = x;
		if (y != null) this.y = y;
		if (width != null) this.width = width;
		if (height != null) this.height = height;
		if (minimized != null) this.minimized = minimized;
		if (opacity != null) this.opacity = opacity;
		if (ghostMode != null) this.ghostMode = ghostMode;
		if (alertEnabled != null) this.alertEnabled = alertEnabled;
	}

}
