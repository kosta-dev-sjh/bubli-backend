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

    @Query(
            value = """
                    SELECT DISTINCT chunk_metadata ->> 'documentLanguage'
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND chunk_metadata ->> 'documentLanguage' IS NOT NULL
                    ORDER BY chunk_metadata ->> 'documentLanguage'
                    """,
            nativeQuery = true
    )
    List<String> findRoomSharedDocumentLanguages(@Param("roomId") UUID roomId);

    @Query(
            value = """
                    SELECT DISTINCT chunk_metadata ->> 'documentLanguage'
                    FROM resource_embeddings
                    WHERE room_id = :roomId
                      AND visibility = 'ROOM_SHARED'
                      AND resource_id IN (:resourceIds)
                      AND chunk_metadata ->> 'documentLanguage' IS NOT NULL
                    ORDER BY chunk_metadata ->> 'documentLanguage'
                    """,
            nativeQuery = true
    )
    List<String> findRoomSharedDocumentLanguagesByResourceIds(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds
    );

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
                      AND (
                          CAST(:documentLanguage AS text) IS NULL
                          OR chunk_metadata ->> 'documentLanguage' IS NULL
                          OR chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                      )
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomShared(
            @Param("roomId") UUID roomId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> searchRoomShared(
            UUID roomId,
            String queryEmbedding,
            int limit
    ) {
        return searchRoomShared(roomId, queryEmbedding, null, limit);
    }

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
                      AND (
                          CAST(:documentLanguage AS text) IS NULL
                          OR chunk_metadata ->> 'documentLanguage' IS NULL
                          OR chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                      )
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchRoomSharedByResourceIds(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> searchRoomSharedByResourceIds(
            UUID roomId,
            List<UUID> resourceIds,
            String queryEmbedding,
            int limit
    ) {
        return searchRoomSharedByResourceIds(roomId, resourceIds, queryEmbedding, null, limit);
    }

    @Query(
            value = """
                    WITH keyword_query AS (
                        SELECT plainto_tsquery(
                            'simple',
                            concat_ws(
                                ' ',
                                NULLIF(:token1, ''),
                                NULLIF(:token2, ''),
                                NULLIF(:token3, ''),
                                NULLIF(:token4, ''),
                                NULLIF(:token5, '')
                            )
                        ) AS tsquery
                    )
                    SELECT
                        embedding.id,
                        embedding.resource_id AS resourceId,
                        embedding.chunk_index AS chunkIndex,
                        embedding.chunk_text AS chunkText,
                        embedding.chunk_metadata::text AS chunkMetadata,
                        LEAST(
                            1.0,
                            (
                                (
                                    CASE WHEN :token1 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token1), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token2 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token2), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token3 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token3), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token4 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token4), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token5 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token5), '%') THEN 1 ELSE 0 END
                                )::double precision / CAST(:tokenCount AS double precision)
                                + ts_rank_cd(to_tsvector('simple', coalesce(embedding.chunk_text, '')), keyword_query.tsquery)
                                + (
                                    GREATEST(
                                        CASE WHEN :token1 <> '' THEN word_similarity(lower(:token1), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token2 <> '' THEN word_similarity(lower(:token2), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token3 <> '' THEN word_similarity(lower(:token3), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token4 <> '' THEN word_similarity(lower(:token4), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token5 <> '' THEN word_similarity(lower(:token5), lower(embedding.chunk_text)) ELSE 0 END
                                    ) * 0.25
                                )
                            )
                        ) AS similarityScore
                    FROM resource_embeddings embedding
                    CROSS JOIN keyword_query
                    WHERE embedding.room_id = :roomId
                      AND embedding.visibility = 'ROOM_SHARED'
                      AND embedding.resource_id IN (:resourceIds)
                      AND (
                          CAST(:documentLanguage AS text) IS NULL
                          OR embedding.chunk_metadata ->> 'documentLanguage' IS NULL
                          OR embedding.chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                      )
                      AND (
                          to_tsvector('simple', coalesce(embedding.chunk_text, '')) @@ keyword_query.tsquery
                          OR (:token1 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token1), '%'))
                          OR (:token2 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token2), '%'))
                          OR (:token3 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token3), '%'))
                          OR (:token4 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token4), '%'))
                          OR (:token5 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token5), '%'))
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
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> searchRoomSharedByResourceIdsAndKeywords(
            UUID roomId,
            List<UUID> resourceIds,
            String token1,
            String token2,
            String token3,
            String token4,
            String token5,
            int tokenCount,
            int limit
    ) {
        return searchRoomSharedByResourceIdsAndKeywords(
                roomId,
                resourceIds,
                token1,
                token2,
                token3,
                token4,
                token5,
                tokenCount,
                null,
                limit
        );
    }

    @Query(
            value = """
                    WITH keyword_query AS (
                        SELECT plainto_tsquery(
                            'simple',
                            concat_ws(
                                ' ',
                                NULLIF(:token1, ''),
                                NULLIF(:token2, ''),
                                NULLIF(:token3, ''),
                                NULLIF(:token4, ''),
                                NULLIF(:token5, '')
                            )
                        ) AS tsquery
                    )
                    SELECT
                        embedding.id,
                        embedding.resource_id AS resourceId,
                        embedding.chunk_index AS chunkIndex,
                        embedding.chunk_text AS chunkText,
                        embedding.chunk_metadata::text AS chunkMetadata,
                        LEAST(
                            1.0,
                            (
                                (
                                    CASE WHEN :token1 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token1), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token2 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token2), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token3 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token3), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token4 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token4), '%') THEN 1 ELSE 0 END
                                    + CASE WHEN :token5 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token5), '%') THEN 1 ELSE 0 END
                                )::double precision / CAST(:tokenCount AS double precision)
                                + ts_rank_cd(to_tsvector('simple', coalesce(embedding.chunk_text, '')), keyword_query.tsquery)
                                + (
                                    GREATEST(
                                        CASE WHEN :token1 <> '' THEN word_similarity(lower(:token1), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token2 <> '' THEN word_similarity(lower(:token2), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token3 <> '' THEN word_similarity(lower(:token3), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token4 <> '' THEN word_similarity(lower(:token4), lower(embedding.chunk_text)) ELSE 0 END,
                                        CASE WHEN :token5 <> '' THEN word_similarity(lower(:token5), lower(embedding.chunk_text)) ELSE 0 END
                                    ) * 0.25
                                )
                            )
                        ) AS similarityScore
                    FROM resource_embeddings embedding
                    CROSS JOIN keyword_query
                    WHERE embedding.room_id = :roomId
                      AND embedding.visibility = 'ROOM_SHARED'
                      AND (
                          CAST(:documentLanguage AS text) IS NULL
                          OR embedding.chunk_metadata ->> 'documentLanguage' IS NULL
                          OR embedding.chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                      )
                      AND (
                          to_tsvector('simple', coalesce(embedding.chunk_text, '')) @@ keyword_query.tsquery
                          OR (:token1 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token1), '%'))
                          OR (:token2 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token2), '%'))
                          OR (:token3 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token3), '%'))
                          OR (:token4 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token4), '%'))
                          OR (:token5 <> '' AND lower(embedding.chunk_text) LIKE concat('%', lower(:token5), '%'))
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
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> searchRoomSharedByKeywords(
            UUID roomId,
            String token1,
            String token2,
            String token3,
            String token4,
            String token5,
            int tokenCount,
            int limit
    ) {
        return searchRoomSharedByKeywords(
                roomId,
                token1,
                token2,
                token3,
                token4,
                token5,
                tokenCount,
                null,
                limit
        );
    }

    @Query(
            value = """
                    WITH ranked_chunks AS (
                        SELECT
                            id,
                            resource_id,
                            chunk_index,
                            chunk_text,
                            chunk_metadata,
                            row_number() OVER (
                                PARTITION BY resource_id
                                ORDER BY chunk_index ASC
                            ) AS resource_chunk_rank
                        FROM resource_embeddings
                        WHERE room_id = :roomId
                          AND visibility = 'ROOM_SHARED'
                          AND resource_id IN (:resourceIds)
                          AND (
                              CAST(:documentLanguage AS text) IS NULL
                              OR chunk_metadata ->> 'documentLanguage' IS NULL
                              OR chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                          )
                    )
                    SELECT
                        id,
                        resource_id AS resourceId,
                        chunk_index AS chunkIndex,
                        chunk_text AS chunkText,
                        chunk_metadata::text AS chunkMetadata,
                        1.0 AS similarityScore
                    FROM ranked_chunks
                    ORDER BY resource_chunk_rank ASC, resource_id ASC, chunk_index ASC
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> findRoomSharedRepresentativeChunks(
            @Param("roomId") UUID roomId,
            @Param("resourceIds") List<UUID> resourceIds,
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> findRoomSharedRepresentativeChunks(
            UUID roomId,
            List<UUID> resourceIds,
            int limit
    ) {
        return findRoomSharedRepresentativeChunks(roomId, resourceIds, null, limit);
    }

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
                      AND (
                          CAST(:documentLanguage AS text) IS NULL
                          OR chunk_metadata ->> 'documentLanguage' IS NULL
                          OR chunk_metadata ->> 'documentLanguage' = CAST(:documentLanguage AS text)
                      )
                    ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<ResourceEmbeddingSearchRow> searchPersonal(
            @Param("ownerId") UUID ownerId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("documentLanguage") String documentLanguage,
            @Param("limit") int limit
    );

    default List<ResourceEmbeddingSearchRow> searchPersonal(
            UUID ownerId,
            String queryEmbedding,
            int limit
    ) {
        return searchPersonal(ownerId, queryEmbedding, null, limit);
    }
}
