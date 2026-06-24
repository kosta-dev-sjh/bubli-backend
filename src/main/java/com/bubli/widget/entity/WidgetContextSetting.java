package com.bubli.widget.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.widget.type.WidgetMode;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "widget_context_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WidgetContextSetting extends BaseTimeEntity {

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

}
