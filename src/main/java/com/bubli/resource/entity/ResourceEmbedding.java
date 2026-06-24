package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import com.bubli.resource.type.ResourceVisibility;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "resource_embeddings",
	uniqueConstraints = @UniqueConstraint(name = "uk_resource_embeddings_resource_chunk", columnNames = {"resource_id", "chunk_index"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceEmbedding extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "resource_id", nullable = false)
	private UUID resourceId;

	@Column(name = "owner_id", nullable = false)
	private UUID ownerId;

	@Column(name = "room_id")
	private UUID roomId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ResourceVisibility visibility;

	@Column(name = "chunk_index", nullable = false)
	private Integer chunkIndex;

	@Column(name = "chunk_text", nullable = false, columnDefinition = "text")
	private String chunkText;

	@Column(nullable = false, columnDefinition = "vector(1024)")
	private String embedding;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "chunk_metadata", columnDefinition = "jsonb")
	private String chunkMetadata;

}
