package com.bubli.resource.entity;

import com.bubli.resource.type.DocumentFileType;
import com.bubli.resource.type.DocumentScope;
import com.bubli.resource.type.DocumentStatus;
import com.bubli.resource.type.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void followsProcessingStateAndDeactivatesChunksWhenDeleted() {
        Document document = createDocument();
        DocumentChunk chunk = document.addChunk(
                0,
                "계약 대금은 검수 완료 후 지급한다.",
                1,
                "대금 지급",
                12
        );

        document.startExtracting();
        document.startIndexing();
        document.markReady();
        document.markDeleted();

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DELETED);
        assertThat(document.isLatest()).isFalse();
        assertThat(document.getDeletedAt()).isNotNull();
        assertThat(chunk.isActive()).isFalse();
    }

    @Test
    void createsNextVersionInSameVersionGroupAndRetiresOldChunks() {
        Document first = createDocument();
        DocumentChunk oldChunk = first.addChunk(0, "이전 내용", 1, null, 3);

        Document second = first.createNextVersion(
                UUID.randomUUID(),
                "contract-v2.pdf",
                DocumentFileType.PDF,
                "documents/contract-v2.pdf",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        );

        assertThat(first.isLatest()).isFalse();
        assertThat(oldChunk.isActive()).isFalse();
        assertThat(second.getVersionGroupId()).isEqualTo(first.getVersionGroupId());
        assertThat(second.getDocumentVersion()).isEqualTo(2);
        assertThat(second.isLatest()).isTrue();
    }

    @Test
    void rejectsInvalidStateTransition() {
        Document document = createDocument();

        assertThatThrownBy(document::markReady)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("현재 상태");
    }

    private Document createDocument() {
        return Document.createFirstVersion(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "contract.pdf",
                DocumentFileType.PDF,
                DocumentType.CONTRACT,
                DocumentScope.PROJECT,
                "documents/contract.pdf",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        );
    }
}
