package com.bubli.resource.service;

import com.bubli.global.ai.AiModelGateway;
import com.bubli.project.service.ProjectRoomAccessPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceSearchHit;
import com.bubli.resource.entity.ResourceEmbeddingSearchRow;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import com.bubli.resource.type.ResourceSearchScope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceSemanticSearchPublicService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;
    private static final int MAX_CANDIDATE_TOP_K = 100;

    private final ResourceEmbeddingRepository resourceEmbeddingRepository;
    private final AiModelGateway aiModelGateway;
    private final EmbeddingVectorFormatter embeddingVectorFormatter;
    private final ProjectRoomAccessPublicService projectRoomAccessService;
    private final ObjectMapper objectMapper;
    private final ResourceSearchMetricsPublicService resourceSearchMetrics;

    public List<String> findRoomSharedDocumentLanguages(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        projectRoomAccessService.requireRoomMember(roomId, userId);
        List<String> languages = resourceIds == null || resourceIds.isEmpty()
                ? resourceEmbeddingRepository.findRoomSharedDocumentLanguages(roomId)
                : resourceEmbeddingRepository.findRoomSharedDocumentLanguagesByResourceIds(
                        roomId,
                        resourceIds.stream().distinct().toList()
                );
        if (languages == null || languages.isEmpty()) {
            return List.of();
        }
        return languages.stream()
                .map(this::normalizeDocumentLanguage)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
    }

    public List<ResourceSearchHit> search(
            UUID userId,
            ResourceSearchScope scope,
            UUID roomId,
            String query,
            Integer topK
    ) {
        String metricScope = scope == ResourceSearchScope.PERSONAL ? "personal" : "room";
        return resourceSearchMetrics.observe("semantic", metricScope, () -> searchSemantic(
                userId,
                scope,
                roomId,
                query,
                topK,
                null,
                false
        ));
    }

    public List<ResourceSearchHit> search(
            UUID userId,
            ResourceSearchScope scope,
            UUID roomId,
            String query,
            Integer topK,
            String documentLanguage
    ) {
        String metricScope = scope == ResourceSearchScope.PERSONAL ? "personal" : "room";
        return resourceSearchMetrics.observe("semantic", metricScope, () -> searchSemantic(
                userId,
                scope,
                roomId,
                query,
                topK,
                documentLanguage,
                true
        ));
    }

    private List<ResourceSearchHit> searchSemantic(
            UUID userId,
            ResourceSearchScope scope,
            UUID roomId,
            String query,
            Integer topK,
            String documentLanguage,
            boolean allowCandidateTopK
    ) {
        //입력 정규화
        ResourceSearchScope normalizedScope = scope == null ? ResourceSearchScope.ROOM_SHARED : scope;
        require(userId, "userId");
        String normalizedQuery = requireText(query, "query");
        //임베딩 모델로 사용자의 쿼리 임베딩
        String queryEmbedding = embeddingVectorFormatter.toVectorLiteral(aiModelGateway.embed(
                "resource-query-embedding",
                normalizedQuery
        ));
        int limit = allowCandidateTopK ? normalizeCandidateTopK(topK) : normalizeTopK(topK);
        String normalizedDocumentLanguage = normalizeDocumentLanguage(documentLanguage);

        //개인 자료일경우
        if (normalizedScope == ResourceSearchScope.PERSONAL) {
            return resourceEmbeddingRepository.searchPersonal(
                            userId,
                            queryEmbedding,
                            normalizedDocumentLanguage,
                            limit
                    )
                    .stream()
                    .map(this::toHit)
                    .toList();
        }
        //프로젝트 룸 멤버인지 확인+ 권한 확인 및 룸 자료일경우
        require(roomId, "roomId");
        projectRoomAccessService.requireRoomMember(roomId, userId);
        return resourceEmbeddingRepository.searchRoomShared(
                        roomId,
                        queryEmbedding,
                        normalizedDocumentLanguage,
                        limit
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    public List<ResourceSearchHit> searchRoomSharedResources(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            String query,
            Integer topK
    ) {
        return searchRoomSharedResources(userId, roomId, resourceIds, query, topK, null);
    }

    public List<ResourceSearchHit> searchRoomSharedResources(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            String query,
            Integer topK,
            String documentLanguage
    ) {
        return resourceSearchMetrics.observe("semantic", "room_resources", () ->
                searchRoomSharedResourcesInternal(
                        userId,
                        roomId,
                        resourceIds,
                        query,
                        topK,
                        documentLanguage
                ));
    }

    private List<ResourceSearchHit> searchRoomSharedResourcesInternal(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            String query,
            Integer topK,
            String documentLanguage
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = requireText(query, "query");
        projectRoomAccessService.requireRoomMember(roomId, userId);
        String queryEmbedding = embeddingVectorFormatter.toVectorLiteral(aiModelGateway.embed(
                "resource-query-embedding",
                normalizedQuery
        ));
        return resourceEmbeddingRepository.searchRoomSharedByResourceIds(
                        roomId,
                        resourceIds.stream().distinct().toList(),
                        queryEmbedding,
                        normalizeDocumentLanguage(documentLanguage),
                        normalizeCandidateTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    public List<ResourceSearchHit> searchRoomSharedResourceKeywords(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            List<String> keywords,
            Integer topK
    ) {
        return searchRoomSharedResourceKeywords(userId, roomId, resourceIds, keywords, topK, null);
    }

    public List<ResourceSearchHit> searchRoomSharedResourceKeywords(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            List<String> keywords,
            Integer topK,
            String documentLanguage
    ) {
        return resourceSearchMetrics.observe("keyword", "room_resources", () ->
                searchRoomSharedResourceKeywordsInternal(
                        userId,
                        roomId,
                        resourceIds,
                        keywords,
                        topK,
                        documentLanguage
                ));
    }

    private List<ResourceSearchHit> searchRoomSharedResourceKeywordsInternal(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            List<String> keywords,
            Integer topK,
            String documentLanguage
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        if (normalizedKeywords.isEmpty()) {
            return List.of();
        }
        projectRoomAccessService.requireRoomMember(roomId, userId);
        return resourceEmbeddingRepository.searchRoomSharedByResourceIdsAndKeywords(
                        roomId,
                        resourceIds.stream().distinct().toList(),
                        keyword(normalizedKeywords, 0),
                        keyword(normalizedKeywords, 1),
                        keyword(normalizedKeywords, 2),
                        keyword(normalizedKeywords, 3),
                        keyword(normalizedKeywords, 4),
                        normalizedKeywords.size(),
                        normalizeDocumentLanguage(documentLanguage),
                        normalizeCandidateTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    public List<ResourceSearchHit> searchRoomSharedKeywords(
            UUID userId,
            UUID roomId,
            List<String> keywords,
            Integer topK
    ) {
        return searchRoomSharedKeywords(userId, roomId, keywords, topK, null);
    }

    public List<ResourceSearchHit> searchRoomSharedKeywords(
            UUID userId,
            UUID roomId,
            List<String> keywords,
            Integer topK,
            String documentLanguage
    ) {
        return resourceSearchMetrics.observe("keyword", "room", () ->
                searchRoomSharedKeywordsInternal(userId, roomId, keywords, topK, documentLanguage));
    }

    private List<ResourceSearchHit> searchRoomSharedKeywordsInternal(
            UUID userId,
            UUID roomId,
            List<String> keywords,
            Integer topK,
            String documentLanguage
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        List<String> normalizedKeywords = normalizeKeywords(keywords);
        if (normalizedKeywords.isEmpty()) {
            return List.of();
        }
        projectRoomAccessService.requireRoomMember(roomId, userId);
        return resourceEmbeddingRepository.searchRoomSharedByKeywords(
                        roomId,
                        keyword(normalizedKeywords, 0),
                        keyword(normalizedKeywords, 1),
                        keyword(normalizedKeywords, 2),
                        keyword(normalizedKeywords, 3),
                        keyword(normalizedKeywords, 4),
                        normalizedKeywords.size(),
                        normalizeDocumentLanguage(documentLanguage),
                        normalizeCandidateTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    public List<ResourceSearchHit> loadRoomSharedResourceChunks(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            Integer topK
    ) {
        return loadRoomSharedResourceChunks(userId, roomId, resourceIds, topK, null);
    }

    public List<ResourceSearchHit> loadRoomSharedResourceChunks(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            Integer topK,
            String documentLanguage
    ) {
        return resourceSearchMetrics.observe("representative", "room_resources", () ->
                loadRoomSharedResourceChunksInternal(userId, roomId, resourceIds, topK, documentLanguage));
    }

    private List<ResourceSearchHit> loadRoomSharedResourceChunksInternal(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            Integer topK,
            String documentLanguage
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        projectRoomAccessService.requireRoomMember(roomId, userId);
        return resourceEmbeddingRepository.findRoomSharedRepresentativeChunks(
                        roomId,
                        resourceIds.stream().distinct().toList(),
                        normalizeDocumentLanguage(documentLanguage),
                        normalizeCandidateTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    private ResourceSearchHit toHit(ResourceEmbeddingSearchRow row) {
        Map<String, Object> metadata = metadata(row.getChunkMetadata());
        return new ResourceSearchHit(
                row.getId(),
                row.getResourceId(),
                row.getChunkIndex(),
                row.getChunkText(),
                integerValue(metadata, "pageNumber"),
                integerValue(metadata, "startLine"),
                integerValue(metadata, "endLine"),
                integerValue(metadata, "startOffset"),
                integerValue(metadata, "endOffset"),
                stringValue(metadata, "originalName"),
                row.getChunkMetadata(),
                row.getSimilarityScore()
        );
    }

    private Map<String, Object> metadata(String chunkMetadata) {
        if (chunkMetadata == null || chunkMetadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    chunkMetadata,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private Integer integerValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String stringValue(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value instanceof String text ? text : null;
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        if (topK < 1) {
            return 1;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    private int normalizeCandidateTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        if (topK < 1) {
            return 1;
        }
        return Math.min(topK, MAX_CANDIDATE_TOP_K);
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(keyword -> keyword.length() >= 2)
                .distinct()
                .limit(5)
                .toList();
    }

    private String keyword(List<String> keywords, int index) {
        return index < keywords.size() ? keywords.get(index) : "";
    }

    private String normalizeDocumentLanguage(String documentLanguage) {
        if (documentLanguage == null || documentLanguage.isBlank()) {
            return null;
        }
        String normalized = documentLanguage.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "ko-kr" -> "ko";
            case "en-us" -> "en";
            case "ja-jp" -> "ja";
            default -> normalized;
        };
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        return value.trim();
    }
}
