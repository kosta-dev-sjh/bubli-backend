package com.bubli.user.entity;

import com.bubli.global.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "user_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(length = 30)
	private String theme;

	@Column(name = "font_scale")
	private Double fontScale;

	@Column(name = "default_room_id")
	private UUID defaultRoomId;

}
