package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.dto.ResourceAnalysisSource;
import com.bubli.resource.entity.Resource;
import com.bubli.resource.entity.ResourceFile;
import com.bubli.resource.repository.AiDocumentRepository;
import com.bubli.resource.repository.ResourceFileRepository;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.resource.repository.ResourceSummaryRepository;
import com.bubli.resource.type.DocumentType;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import com.bubli.storage.service.StoragePublicService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAnalysisPublicServiceTest {

    @Test
    void loadsMarkdownAnalysisSourceAsTextDocument() {
        UUID resourceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Resource resource = resource(resourceId, roomId);
        ResourceFile file = file(resourceId, "requirements.md", "text/markdown; charset=utf-8");
        ResourceAnalysisPublicService service = service(resource, file, """
                # Requirements

                - Payment approval
                """.getBytes(StandardCharsets.UTF_8));

        ResourceAnalysisSource source = service.loadAnalysisSourceForJob(resourceId);

        assertThat(source.resourceId()).isEqualTo(resourceId);
        assertThat(source.mimeType()).isEqualTo("text/markdown; charset=utf-8");
        assertThat(source.documentType()).isEqualTo(DocumentType.REQUIREMENT);
        assertThat(source.text()).contains("Payment approval");
        assertThat(source.pageCount()).isEqualTo(1);
    }

    @Test
    void loadsDocxAnalysisSourceWithParagraphText() throws IOException {
        UUID resourceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Resource resource = resource(resourceId, roomId);
        ResourceFile file = file(
                resourceId,
                "contract.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        ResourceAnalysisPublicService service = service(resource, file, docx("Contract amount is 1000000 KRW."));

        ResourceAnalysisSource source = service.loadAnalysisSourceForJob(resourceId);

        assertThat(source.documentType()).isEqualTo(DocumentType.CONTRACT);
        assertThat(source.text()).contains("Contract amount is 1000000 KRW.");
        assertThat(source.pages()).hasSize(1);
        assertThat(source.characterCount()).isGreaterThan(0);
    }

    @Test
    void rejectsUnsupportedAnalysisFormatWithResource415() {
        UUID resourceId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Resource resource = resource(resourceId, roomId);
        ResourceFile file = file(resourceId, "sheet.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        ResourceAnalysisPublicService service = service(resource, file, "binary".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> service.loadAnalysisSourceForJob(resourceId))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_415_001)
                );
        assertThat(resource.getStatus()).isEqualTo(ResourceStatus.FAILED);
    }

    private ResourceAnalysisPublicService service(Resource resource, ResourceFile file, byte[] content) {
        ResourceRepository resourceRepository = mock(ResourceRepository.class);
        ResourceFileRepository resourceFileRepository = mock(ResourceFileRepository.class);
        StoragePublicService storageService = mock(StoragePublicService.class);

        when(resourceRepository.findById(resource.getId())).thenReturn(Optional.of(resource));
        when(resourceFileRepository.findTopByResourceIdOrderByCreatedAtDesc(resource.getId()))
                .thenReturn(Optional.of(file));
        when(storageService.open(file.getStorageKey())).thenReturn(new ByteArrayInputStream(content));

        return new ResourceAnalysisPublicService(
                resourceRepository,
                resourceFileRepository,
                mock(ResourceSummaryRepository.class),
                mock(AiDocumentRepository.class),
                mock(ResourceEmbeddingIndexPublicService.class),
                mock(ResourceRelationIndexPublicService.class),
                storageService
        );
    }

    private Resource resource(UUID resourceId, UUID roomId) {
        Resource resource = Resource.create(
                UUID.randomUUID(),
                roomId,
                "document",
                ResourceKind.FILE,
                ResourceVisibility.ROOM_SHARED,
                ResourceStatus.READY
        );
        ReflectionTestUtils.setField(resource, "id", resourceId);
        return resource;
    }

    private ResourceFile file(UUID resourceId, String originalName, String mimeType) {
        return ResourceFile.create(
                resourceId,
                originalName,
                mimeType,
                100,
                "resources/%s/%s".formatted(resourceId, originalName),
                "checksum"
        );
    }

    private byte[] docx(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
