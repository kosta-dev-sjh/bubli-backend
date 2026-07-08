package com.bubli.agent.service;

import com.bubli.agent.dto.GeneratedDocumentResponse;
import com.bubli.agent.dto.GeneratedDocumentExportResult;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.entity.GeneratedDocument;
import com.bubli.agent.repository.GeneratedDocumentRepository;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GeneratedDocumentService {

    private static final String DEFAULT_DOCUMENT_TYPE = "GENERAL";

    private final GeneratedDocumentRepository generatedDocumentRepository;
    private final ProjectMembershipPublicService projectMembershipPublicService;

    @Transactional
    public GeneratedDocument createFromSuggestion(UUID reviewerId, AgentSuggestion suggestion) {
        return generatedDocumentRepository.findBySuggestionId(suggestion.getId())
                .orElseGet(() -> generatedDocumentRepository.save(toGeneratedDocument(reviewerId, suggestion)));
    }

    @Transactional(readOnly = true)
    public GeneratedDocumentResponse get(UUID userId, UUID documentId) {
        GeneratedDocument document = getReadableDocument(userId, documentId);
        return GeneratedDocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public GeneratedDocumentExportResult exportMarkdown(UUID userId, UUID documentId) {
        GeneratedDocument document = getReadableDocument(userId, documentId);
        return GeneratedDocumentExportResult.markdown(document.getTitle(), exportContentMarkdown(document));
    }

    @Transactional
    public void delete(UUID userId, UUID documentId) {
        GeneratedDocument document = getReadableDocument(userId, documentId);
        generatedDocumentRepository.delete(document);
    }

    @Transactional(readOnly = true)
    public PageResponse<GeneratedDocumentResponse> getMine(UUID userId, Pageable pageable) {
        Page<GeneratedDocument> page = generatedDocumentRepository.findByUserIdAndRoomIdIsNull(userId, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<GeneratedDocumentResponse> getRoomDocuments(UUID userId, UUID roomId, Pageable pageable) {
        projectMembershipPublicService.assertActiveMember(userId, roomId);
        Page<GeneratedDocument> page = generatedDocumentRepository.findByRoomId(roomId, pageable);
        return toPageResponse(page);
    }

    private GeneratedDocument toGeneratedDocument(UUID reviewerId, AgentSuggestion suggestion) {
        Map<String, Object> payload = suggestion.getPayloadJson();
        return GeneratedDocument.create(
                suggestion.getUserId(),
                suggestion.getRoomId(),
                suggestion.getId(),
                suggestion.getResourceId(),
                title(payload),
                documentType(payload),
                contentMarkdown(payload),
                metadata(reviewerId, suggestion, payload)
        );
    }

    private void validateAccess(UUID userId, GeneratedDocument document) {
        if (document.getRoomId() != null) {
            projectMembershipPublicService.assertActiveMember(userId, document.getRoomId());
            return;
        }
        if (!document.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.AGENT_404_002);
        }
    }

    private GeneratedDocument getReadableDocument(UUID userId, UUID documentId) {
        GeneratedDocument document = generatedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_002));
        validateAccess(userId, document);
        return document;
    }

    private PageResponse<GeneratedDocumentResponse> toPageResponse(Page<GeneratedDocument> page) {
        List<GeneratedDocumentResponse> items = page.getContent().stream()
                .map(GeneratedDocumentResponse::from)
                .toList();
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    private String title(Map<String, Object> payload) {
        String title = text(payload.get("title"));
        if (title != null) {
            return title;
        }
        String documentType = documentType(payload);
        return documentType + " draft";
    }

    private String documentType(Map<String, Object> payload) {
        String documentType = text(payload.get("documentType"));
        if (documentType == null) {
            documentType = text(payload.get("type"));
        }
        return documentType == null ? DEFAULT_DOCUMENT_TYPE : documentType;
    }

    private String contentMarkdown(Map<String, Object> payload) {
        String content = text(payload.get("contentMarkdown"));
        if (content == null) {
            content = text(payload.get("markdown"));
        }
        if (content == null) {
            content = text(payload.get("draftMarkdown"));
        }
        if (content == null) {
            content = nestedText(payload.get("document"), "contentMarkdown");
        }
        if (content == null) {
            content = nestedText(payload.get("draft"), "contentMarkdown");
        }
        if (content == null) {
            content = text(payload.get("content"));
        }
        if (content == null) {
            throw new BusinessException(ErrorCode.AGENT_400_001);
        }
        return content;
    }

    private String exportContentMarkdown(GeneratedDocument document) {
        String content = text(document.getContentMarkdown());
        if (content != null) {
            return content;
        }
        Map<String, Object> metadata = document.getMetadataJson();
        if (metadata != null) {
            content = text(metadata.get("contentMarkdown"));
            if (content == null) {
                content = text(metadata.get("content"));
            }
            if (content == null) {
                content = text(metadata.get("agentResponse"));
            }
        }
        if (content != null) {
            return content;
        }
        return "# %s".formatted(document.getTitle());
    }

    private Map<String, Object> metadata(UUID reviewerId, AgentSuggestion suggestion, Map<String, Object> payload) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", "AGENT_SUGGESTION");
        metadata.put("reviewerId", reviewerId.toString());
        metadata.put("jobId", suggestion.getJobId() == null ? null : suggestion.getJobId().toString());
        metadata.put("sourceResourceIds", payload.getOrDefault("sourceResourceIds", List.of()));
        metadata.put("instruction", payload.get("instruction"));
        metadata.put("agentResponse", payload.get("agentResponse"));
        return metadata;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private String nestedText(Object value, String key) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        return text(map.get(key));
    }
}
