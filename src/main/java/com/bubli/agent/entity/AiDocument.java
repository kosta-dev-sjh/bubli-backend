package com.bubli.agent.entity;

import com.bubli.agent.type.AiDocumentStatus;
import com.bubli.agent.type.AiDocumentType;
import java.math.BigDecimal;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "ai_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDocument {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false, unique = true)
	private UUID resourceId;

	@Column(name = "room_id")
	private UUID roomId;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 40)
	private AiDocumentType documentType;

	@Column(name = "detected_confidence", precision = 5, scale = 4)
	private BigDecimal detectedConfidence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiDocumentStatus status;

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
