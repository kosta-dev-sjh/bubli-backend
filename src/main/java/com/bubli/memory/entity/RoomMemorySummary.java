package com.bubli.memory.entity;

import com.bubli.memory.type.SummaryStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "room_memory_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMemorySummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "room_id", nullable = false)
	private UUID roomId;

	@Column(name = "from_sequence", nullable = false)
	private Long fromSequence;

	@Column(name = "to_sequence", nullable = false)
	private Long toSequence;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
	private String summaryJson;

	@Column(name = "created_by_user_id")
	private UUID createdByUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SummaryStatus status = SummaryStatus.DRAFT;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public static RoomMemorySummary createDraft(
			UUID roomId,
			Long fromSequence,
			Long toSequence,
			String summaryJson,
			UUID createdByUserId
	) {
		RoomMemorySummary summary = new RoomMemorySummary();
		summary.roomId = require(roomId, "roomId");
		summary.fromSequence = require(fromSequence, "fromSequence");
		summary.toSequence = require(toSequence, "toSequence");
		summary.summaryJson = requireText(summaryJson, "summaryJson");
		summary.createdByUserId = createdByUserId;
		summary.status = SummaryStatus.DRAFT;
		return summary;
	}

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

	private static <T> T require(T value, String field) {
		if (value == null) {
			throw new IllegalArgumentException(field + " is required.");
		}
		return value;
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required.");
		}
		return value;
	}

}
