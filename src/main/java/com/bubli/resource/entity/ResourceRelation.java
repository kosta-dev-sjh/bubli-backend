package com.bubli.resource.entity;

import java.math.BigDecimal;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_relations",
	uniqueConstraints = @UniqueConstraint(name = "uk_resource_relations_pair", columnNames = {"resource_id", "related_resource_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceRelation {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "related_resource_id", nullable = false)
	private UUID relatedResourceId;

	@Column(columnDefinition = "text")
	private String reason;

	@Column(precision = 8, scale = 5)
	private BigDecimal score;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	private void onCreate() {
		this.createdAt = Instant.now();
	}

}
