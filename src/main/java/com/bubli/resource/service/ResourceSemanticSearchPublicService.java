package com.bubli.resource.service;

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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceSemanticSearchPublicService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int MAX_TOP_K = 20;

    private final ResourceEmbeddingRepository resourceEmbeddingRepository;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final EmbeddingVectorFormatter embeddingVectorFormatter;
    private final ProjectRoomAccessPublicService projectRoomAccessService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ResourceSearchHit> search(
            UUID userId,
            ResourceSearchScope scope,
            UUID roomId,
            String query,
            Integer topK
    ) {
        //입력 정규화
        ResourceSearchScope normalizedScope = scope == null ? ResourceSearchScope.ROOM_SHARED : scope;
        require(userId, "userId");
        String normalizedQuery = requireText(query, "query");
        //가용모델 확인
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not available. Enable the ai profile to search resources.");
        }
        //임베딩 모델로 사용자의 쿼리 임베딩
        String queryEmbedding = embeddingVectorFormatter.toVectorLiteral(embeddingModel.embed(normalizedQuery));
        int limit = normalizeTopK(topK);

        //개인 자료일경우
        if (normalizedScope == ResourceSearchScope.PERSONAL) {
            return resourceEmbeddingRepository.searchPersonal(userId, queryEmbedding, limit)
                    .stream()
                    .map(this::toHit)
                    .toList();
        }
        //프로젝트 룸 멤버인지 확인+ 권한 확인 및 룸 자료일경우
        require(roomId, "roomId");
        projectRoomAccessService.requireRoomMember(roomId, userId);
        return resourceEmbeddingRepository.searchRoomShared(roomId, queryEmbedding, limit)
                .stream()
                .map(this::toHit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceSearchHit> searchRoomSharedResources(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            String query,
            Integer topK
    ) {
        require(userId, "userId");
        require(roomId, "roomId");
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = requireText(query, "query");
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("EmbeddingModel is not available. Enable the ai profile to search resources.");
        }
        projectRoomAccessService.requireRoomMember(roomId, userId);
        String queryEmbedding = embeddingVectorFormatter.toVectorLiteral(embeddingModel.embed(normalizedQuery));
        return resourceEmbeddingRepository.searchRoomSharedByResourceIds(
                        roomId,
                        resourceIds.stream().distinct().toList(),
                        queryEmbedding,
                        normalizeTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceSearchHit> searchRoomSharedResourceKeywords(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            List<String> keywords,
            Integer topK
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
                        normalizeTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceSearchHit> searchRoomSharedKeywords(
            UUID userId,
            UUID roomId,
            List<String> keywords,
            Integer topK
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
                        normalizeTopK(topK)
                )
                .stream()
                .map(this::toHit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceSearchHit> loadRoomSharedResourceChunks(
            UUID userId,
            UUID roomId,
            List<UUID> resourceIds,
            Integer topK
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
                        normalizeTopK(topK)
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
