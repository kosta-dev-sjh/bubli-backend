package com.bubli.widget.entity;

import com.bubli.widget.type.WidgetMode;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "widget_context_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetContextSetting {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(name = "selected_room_id")
	private UUID selectedRoomId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private WidgetMode mode = WidgetMode.PERSONAL;

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

}
