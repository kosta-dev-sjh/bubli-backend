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

    private ResourceSearchHit toHit(ResourceEmbeddingSearchRow row) {
        return new ResourceSearchHit(
                row.getId(),
                row.getResourceId(),
                row.getChunkIndex(),
                row.getChunkText(),
                pageNumber(row.getChunkMetadata()),
                row.getChunkMetadata(),
                row.getSimilarityScore()
        );
    }

    private Integer pageNumber(String chunkMetadata) {
        if (chunkMetadata == null || chunkMetadata.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> metadata = objectMapper.readValue(
                    chunkMetadata,
                    new TypeReference<>() {
                    }
            );
            Object value = metadata.get("pageNumber");
            if (value instanceof Number number) {
                return number.intValue();
            }
            return null;
        } catch (JsonProcessingException e) {
            return null;
        }
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
