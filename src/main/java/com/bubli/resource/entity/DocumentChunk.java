package com.bubli.resource.entity;

import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_document_chunks_document_index",
                        columnNames = {"document_id", "chunk_index"}
                )
        },
        indexes = {
                @Index(name = "idx_document_chunks_document_active", columnList = "document_id,active"),
                @Index(name = "idx_document_chunks_vector_store_id", columnList = "vector_store_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentChunk extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "vector_store_id")
    private UUID vectorStoreId;
    //다른 버전 문서가 있어도 청크삭제가 아닌 비활성화
    @Column(name = "active", nullable = false)
    private boolean active;

    private DocumentChunk(
            Document document,
            int chunkIndex,
            String content,
            Integer pageNumber,
            String sectionTitle,
            int tokenCount
    ) {
        if (document == null) {
            throw new IllegalArgumentException("document는 필수입니다.");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("chunkIndex는 0 이상이어야 합니다.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수입니다.");
        }
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber는 1 이상이어야 합니다.");
        }
        if (tokenCount < 1) {
            throw new IllegalArgumentException("tokenCount는 1 이상이어야 합니다.");
        }
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.pageNumber = pageNumber;
        this.sectionTitle = sectionTitle;
        this.tokenCount = tokenCount;
        this.active = true;
    }

    static DocumentChunk create(
            Document document,
            int chunkIndex,
            String content,
            Integer pageNumber,
            String sectionTitle,
            int tokenCount
    ) {
        return new DocumentChunk(
                document,
                chunkIndex,
                content,
                pageNumber,
                sectionTitle,
                tokenCount
        );
    }

    public void linkVectorStore(UUID vectorStoreId) {
        if (vectorStoreId == null) {
            throw new IllegalArgumentException("vectorStoreId는 필수입니다.");
        }
        this.vectorStoreId = vectorStoreId;
    }

    public void deactivate() {
        active = false;
    }
}
