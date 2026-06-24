package com.bubli.widget.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.widget.type.BubbleType;
import java.math.BigDecimal;

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
public class WidgetBubbleSetting extends BaseTimeEntity {

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

}
