package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceEmbeddingSearchRow;
import com.bubli.resource.type.ResourceVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceEmbeddingRepository extends JpaRepository<ResourceEmbedding, UUID> {

    List<ResourceEmbedding> findAllByResourceIdOrderByChunkIndex(UUID resourceId);

    void deleteAllByResourceId(UUID resourceId);

    List<ResourceEmbedding> findAllByOwnerIdAndVisibility(UUID ownerId, ResourceVisibility visibility);

    List<ResourceEmbedding> findAllByRoomIdAndVisibility(UUID roomId, ResourceVisibility visibility);

    @Modifying
    @Query(
            value = """
                    INSERT INTO resource_embeddings (
                        id,
                        resource_id,
                        owner_id,
                        room_id,
                        visibility,
                        chunk_index,
                        chunk_text,
                        embedding,
                        chunk_metadata,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :id,
                        :resourceId,
                        :ownerId,
                        :roomId,
                        :visibility,
                        :chunkIndex,
                        :chunkText,
                        CAST(:embedding AS vector),
                        CAST(:chunkMetadata AS jsonb),
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP
                    )
                    """,
            nativeQuery = true
    )
    void insertEmbedding(
            @Param("id") UUID id,
            @Param("resourceId") UUID resourceId,
            @Param("ownerId") UUID ownerId,
            @Param("roomId") UUID roomId,
            @Param("visibility") String visibility,
            @Param("chunkIndex") int chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding,
            @Param("chunkMetadata") String chunkMetadata
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
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND resource_id IN (:resourceIds)
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomSharedByResourceIds(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds,
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
                        (
                            (
                                CASE WHEN :token1 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token1), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token2 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token2), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token3 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token3), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token4 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token4), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token5 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token5), '%') THEN 1 ELSE 0 END
                            )::double precision / CAST(:tokenCount AS double precision)
                        ) AS similarityScore
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND resource_id IN (:resourceIds)
                      AND (
                          (:token1 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token1), '%'))
                          OR (:token2 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token2), '%'))
                          OR (:token3 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token3), '%'))
                          OR (:token4 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token4), '%'))
                          OR (:token5 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token5), '%'))
                      )
                    ORDER BY similarityScore DESC, chunk_index ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomSharedByResourceIdsAndKeywords(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds,
            @Param("token1") String token1,
            @Param("token2") String token2,
            @Param("token3") String token3,
            @Param("token4") String token4,
            @Param("token5") String token5,
            @Param("tokenCount") int tokenCount,
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
                        (
                            (
                                CASE WHEN :token1 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token1), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token2 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token2), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token3 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token3), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token4 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token4), '%') THEN 1 ELSE 0 END
                                + CASE WHEN :token5 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token5), '%') THEN 1 ELSE 0 END
                            )::double precision / CAST(:tokenCount AS double precision)
                        ) AS similarityScore
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND (
                          (:token1 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token1), '%'))
                          OR (:token2 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token2), '%'))
                          OR (:token3 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token3), '%'))
                          OR (:token4 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token4), '%'))
                          OR (:token5 <> '' AND lower(chunk_text) LIKE concat('%', lower(:token5), '%'))
                      )
                    ORDER BY similarityScore DESC, chunk_index ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomSharedByKeywords(
            @Param("roomId") UUID roomId,
            @Param("token1") String token1,
            @Param("token2") String token2,
            @Param("token3") String token3,
            @Param("token4") String token4,
            @Param("token5") String token5,
            @Param("tokenCount") int tokenCount,
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
                        1.0 AS similarityScore
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND resource_id IN (:resourceIds)
                    ORDER BY resource_id ASC, chunk_index ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> findRoomSharedRepresentativeChunks(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds,
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
