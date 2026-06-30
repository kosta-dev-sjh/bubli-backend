package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.resource.type.DocumentFileType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class DocumentFileInspector {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;
    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public InspectedDocument inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.RESOURCE_413_001);
        }

        String fileName = safeOriginalFileName(file.getOriginalFilename());
        DocumentFileType fileType = fileType(fileName);
        validateContent(file, fileType);

        return new InspectedDocument(fileName, fileType, checksum(file));
    }

    private void validateContent(MultipartFile file, DocumentFileType fileType) {
        try (InputStream input = file.getInputStream()) {
            if (fileType == DocumentFileType.PDF) {
                byte[] header = input.readNBytes(PDF_MAGIC.length);
                if (!MessageDigest.isEqual(header, PDF_MAGIC)) {
                    throw new BusinessException(ErrorCode.RESOURCE_415_001);
                }
                return;
            }
            if (fileType == DocumentFileType.DOCX) {
                try (XWPFDocument ignored = new XWPFDocument(input)) {
                    return;
                } catch (IOException | RuntimeException e) {
                    throw new BusinessException(ErrorCode.RESOURCE_415_001);
                }
            }

            byte[] content = input.readAllBytes();
            if (containsNullByte(content)) {
                throw new BusinessException(ErrorCode.RESOURCE_415_001);
            }
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content));
        } catch (CharacterCodingException e) {
            throw new BusinessException(ErrorCode.RESOURCE_415_001);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private String checksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream input = new DigestInputStream(file.getInputStream(), digest)) {
                input.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESOURCE_500_001);
        }
    }

    private DocumentFileType fileType(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return DocumentFileType.PDF;
        }
        if (lowerName.endsWith(".txt")) {
            return DocumentFileType.TXT;
        }
        if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
            return DocumentFileType.MARKDOWN;
        }
        if (lowerName.endsWith(".docx")) {
            return DocumentFileType.DOCX;
        }
        throw new BusinessException(ErrorCode.RESOURCE_415_001);
    }

    private String safeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank() || fileName.length() > 255) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }
        return fileName;
    }

    private boolean containsNullByte(byte[] content) {
        for (byte value : content) {
            if (value == 0) {
                return true;
            }
        }
        return false;
    }

    public record InspectedDocument(
            String fileName,
            DocumentFileType fileType,
            String checksum
    ) {
    }
}
