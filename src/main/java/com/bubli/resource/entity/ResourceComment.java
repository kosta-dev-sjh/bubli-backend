package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_comments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceComment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "author_id", nullable = false)
	private UUID authorId;

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(nullable = false, columnDefinition = "text")
	private String body;

	@Column(name = "deleted_at")
	private Instant deletedAt;

}
