package com.bubli.personal.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.personal.type.MemoStatus;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "memos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "author_user_id", nullable = false)
	private UUID authorUserId;

	@Column(name = "room_id")
	private UUID roomId;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MemoStatus status = MemoStatus.ACTIVE;

}
