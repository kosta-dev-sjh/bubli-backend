package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.type.DocumentFileType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentFileInspectorTest {

    private final DocumentFileInspector inspector = new DocumentFileInspector();

    @Test
    void acceptsPdfByExtensionAndMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                "application/pdf",
                "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII)
        );

        DocumentFileInspector.InspectedDocument inspected = inspector.inspect(file);

        assertThat(inspected.fileName()).isEqualTo("contract.pdf");
        assertThat(inspected.fileType()).isEqualTo(DocumentFileType.PDF);
        assertThat(inspected.checksum()).hasSize(64);
    }

    @Test
    void stripsClientPathFromOriginalFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\fakepath\\requirements.txt",
                "text/plain",
                "요구사항".getBytes(StandardCharsets.UTF_8)
        );

        assertThat(inspector.inspect(file).fileName()).isEqualTo("requirements.txt");
    }

    @Test
    void rejectsPdfExtensionWithNonPdfContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> inspector.inspect(file))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_415_001)
                );
    }

    @Test
    void rejectsBinaryTxt() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "binary.txt",
                "text/plain",
                new byte[]{'a', 0, 'b'}
        );

        assertThatThrownBy(() -> inspector.inspect(file))
                .isInstanceOf(BusinessException.class);
    }
}
