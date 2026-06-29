package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.ResourceEmbeddingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//코드 흐름
//pages (TextPage 리스트)
//     ↓
//textChunker.splitPages()     → 청크 분할 (TextChunk 리스트)
//     ↓
//deleteAllByResourceId()      → 기존 임베딩 전부 삭제
//     ↓
//embeddingModel.embed(text)   → float[] 벡터 생성 (AI 모델 호출)
//     ↓
//toEmbedding()                → ResourceEmbedding 엔티티 매핑
//     ↓
//saveAll()                    → DB(pgvector) 저장
//     ↓
//IndexResult.indexed(n)       → 결과 반환




@Service
@RequiredArgsConstructor
public class ResourceEmbeddingIndexPublicService {

    private final ResourceEmbeddingRepository resourceEmbeddingRepository;
    private final TextChunker textChunker;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final EmbeddingVectorFormatter embeddingVectorFormatter;

    public IndexResult index(Resource resource, ResourceFile resourceFile, String text) {
        return index(resource, resourceFile, List.of(new TextChunker.TextPage(null, text)));
    }

    public IndexResult index(Resource resource, ResourceFile resourceFile, List<TextChunker.TextPage> pages) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            return IndexResult.skipped();
        }
        //청크 페이지 나눈걸로
        List<TextChunker.TextChunk> chunks = textChunker.splitPages(pages);
        //기존 임베딩 삭제, 분석시 이전 임베딩 영향 안받기위해
        resourceEmbeddingRepository.deleteAllByResourceId(resource.getId());

        List<ResourceEmbedding> embeddings = chunks.stream()
                .map(chunk -> toEmbedding(resource, resourceFile, chunk, embeddingModel.embed(chunk.text())))
                .toList();
        resourceEmbeddingRepository.saveAll(embeddings);
        return IndexResult.indexed(embeddings.size());
    }

    private ResourceEmbedding toEmbedding(
            Resource resource,
            ResourceFile resourceFile,
            TextChunker.TextChunk chunk,
            float[] embedding
    ) {
        return ResourceEmbedding.create(
                resource.getId(),
                resource.getOwnerId(),
                resource.getRoomId(),
                resource.getVisibility(),
                chunk.index(),
                chunk.text(),
                embeddingVectorFormatter.toVectorLiteral(embedding),
                metadata(resourceFile, chunk)
        );
    }

    private Map<String, Object> metadata(ResourceFile resourceFile, TextChunker.TextChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalName", resourceFile.getOriginalName());
        metadata.put("mimeType", resourceFile.getMimeType());
        metadata.put("pageNumber", chunk.pageNumber());
        metadata.put("startOffset", chunk.startOffset());
        metadata.put("endOffset", chunk.endOffset());
        metadata.put("characterCount", chunk.text().length());
        return metadata;
    }

    public record IndexResult(
            boolean indexed,
            int chunkCount
    ) {

        static IndexResult indexed(int chunkCount) {
            return new IndexResult(true, chunkCount);
        }

        static IndexResult skipped() {
            return new IndexResult(false, 0);
        }
    }
}
