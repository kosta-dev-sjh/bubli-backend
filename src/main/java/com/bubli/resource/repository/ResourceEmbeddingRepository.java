package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceEmbeddingSearchRow;
import com.bubli.resource.type.ResourceVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceEmbeddingRepository extends JpaRepository<ResourceEmbedding, UUID> {

    List<ResourceEmbedding> findAllByResourceIdOrderByChunkIndex(UUID resourceId);

    void deleteAllByResourceId(UUID resourceId);

    List<ResourceEmbedding> findAllByOwnerIdAndVisibility(UUID ownerId, ResourceVisibility visibility);

    List<ResourceEmbedding> findAllByRoomIdAndVisibility(UUID roomId, ResourceVisibility visibility);

    @Query(
            value = """
                    SELECT
                        id,
                        resource_id AS resourceId,
                        chunk_index AS chunkIndex,
                        chunk_text AS chunkText,
                        chunk_metadata::text AS chunkMetadata,
                        1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarityScore
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomShared(
            @Param("roomId") UUID roomId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );

    @Query(
            value = """
                    SELECT
                        id,
                        resource_id AS resourceId,
                        chunk_index AS chunkIndex,
                        chunk_text AS chunkText,
                        chunk_metadata::text AS chunkMetadata,
                        1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarityScore
                    FROM resource_embeddings
                    WHERE owner_id = :ownerId
                      AND visibility = 'PERSONAL'
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchPersonal(
            @Param("ownerId") UUID ownerId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit
    );
}
