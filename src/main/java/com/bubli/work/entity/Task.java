package com.bubli.work.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.work.type.TaskStatus;
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
public class Task extends BaseTimeEntity {

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

}
