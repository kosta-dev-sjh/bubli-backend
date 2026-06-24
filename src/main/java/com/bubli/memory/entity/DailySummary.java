package com.bubli.memory.entity;

import com.bubli.memory.type.SummaryStatus;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "daily_summaries",
	uniqueConstraints = @UniqueConstraint(name = "uk_daily_summaries_user_date", columnNames = {"user_id", "summary_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySummary {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "summary_json", nullable = false, columnDefinition = "jsonb")
	private String summaryJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SummaryStatus status = SummaryStatus.DRAFT;

	@Column(name = "approved_at")
	private Instant approvedAt;

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
