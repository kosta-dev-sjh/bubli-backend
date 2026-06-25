package com.bubli.resource.service;

import com.bubli.resource.entity.Document;
import com.bubli.resource.entity.DocumentChunk;
import com.bubli.resource.repository.DocumentRepository;
import com.bubli.resource.type.DocumentFileType;
import com.bubli.resource.type.DocumentScope;
import com.bubli.resource.type.DocumentType;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentLifecycleServiceTest {

    @Test
    void flushesRetiredVersionBeforeSavingNewLatestVersion() {
        DocumentRepository repository = mock(DocumentRepository.class);
        Document current = createDocument();
        UUID vectorStoreId = UUID.randomUUID();
        DocumentChunk chunk = current.addChunk(0, "이전 계약 내용", 1, null, 5);
        chunk.linkVectorStore(vectorStoreId);

        when(repository.findLatestVersionForUpdate(current.getVersionGroupId()))
                .thenReturn(Optional.of(current));
        when(repository.save(org.mockito.ArgumentMatchers.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentVersionChange result = new DocumentLifecycleService(repository).createNextVersion(
                current.getVersionGroupId(),
                UUID.randomUUID(),
                "contract-v2.pdf",
                DocumentFileType.PDF,
                "documents/contract-v2.pdf",
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
        );

        InOrder order = inOrder(repository);
        order.verify(repository).saveAndFlush(current);
        order.verify(repository).save(result.newDocument());
        assertThat(result.retiredVectorStoreIds()).containsExactly(vectorStoreId);
        assertThat(current.isLatest()).isFalse();
        assertThat(chunk.isActive()).isFalse();
    }

    @Test
    void returnsVectorIdsThatIndexingServiceMustDelete() {
        DocumentRepository repository = mock(DocumentRepository.class);
        Document document = createDocument();
        UUID vectorStoreId = UUID.randomUUID();
        DocumentChunk chunk = document.addChunk(0, "삭제 대상", 1, null, 3);
        chunk.linkVectorStore(vectorStoreId);
        UUID documentId = UUID.randomUUID();

        when(repository.findByIdForUpdate(documentId)).thenReturn(Optional.of(document));

        assertThat(new DocumentLifecycleService(repository).markDeleted(documentId))
                .containsExactly(vectorStoreId);
        assertThat(document.isLatest()).isFalse();
        assertThat(chunk.isActive()).isFalse();
        verify(repository).saveAndFlush(document);
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
