package com.bubli.memory.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.memory.type.SummaryStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "room_memory_summaries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMemorySummary extends BaseTimeEntity {

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

}
