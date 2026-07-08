package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.dto.GeneratedDocumentExportResult;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionQueryService {

    private final AgentSuggestionRepository agentSuggestionRepository;
    private final ProjectMembershipPublicService projectMembershipPublicService;

    private static final List<AgentSuggestionType> CONFIRMATION_ITEM_TYPES = List.of(
            AgentSuggestionType.QUESTION,
            AgentSuggestionType.REVIEW_ITEM,
            AgentSuggestionType.CONTRACT_FIELD,
            AgentSuggestionType.CONTRACT_REVIEW
    );

    private static final List<AgentSuggestionType> CONTRACT_REFERENCE_TYPES = List.of(
            AgentSuggestionType.CONTRACT_FIELD,
            AgentSuggestionType.CONTRACT_REVIEW
    );

    @Transactional(readOnly = true)
    public List<AgentSuggestionResponse> findMine(
            UUID userId,
            AgentSuggestionStatus status,
            AgentSuggestionType suggestionType
    ) {
        //userId 기반 status, suggestionType별로 저장
        return agentSuggestionRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(suggestion -> status == null || suggestion.getStatus() == status)
                .filter(suggestion -> suggestionType == null || suggestion.getSuggestionType() == suggestionType)
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSuggestionResponse> findRoomSuggestions(
            UUID userId,
            UUID roomId,
            AgentSuggestionStatus status,
            AgentSuggestionType suggestionType
    ) {
        projectMembershipPublicService.assertActiveMember(userId, roomId);
        List<AgentSuggestion> suggestions = status == null
                ? agentSuggestionRepository.findAllByRoomIdOrderByCreatedAtDesc(roomId)
                : agentSuggestionRepository.findAllByRoomIdAndStatusOrderByCreatedAtDesc(roomId, status);

        return suggestions.stream()
                .filter(suggestion -> suggestionType == null || suggestion.getSuggestionType() == suggestionType)
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSuggestionResponse> findRoomConfirmationItems(
            UUID userId,
            UUID roomId,
            AgentSuggestionStatus status
    ) {
        projectMembershipPublicService.assertActiveMember(userId, roomId);
        List<AgentSuggestion> suggestions = status == null
                ? agentSuggestionRepository.findAllByRoomIdAndSuggestionTypeInOrderByCreatedAtDesc(
                roomId,
                CONFIRMATION_ITEM_TYPES
        )
                : agentSuggestionRepository.findAllByRoomIdAndSuggestionTypeInAndStatusOrderByCreatedAtDesc(
                roomId,
                CONFIRMATION_ITEM_TYPES,
                status
        );

        return suggestions.stream()
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSuggestionResponse> findRoomConfirmedRequirements(UUID userId, UUID roomId) {
        projectMembershipPublicService.assertActiveMember(userId, roomId);
        return agentSuggestionRepository.findAllByRoomIdAndSuggestionTypeAndStatus(
                        roomId,
                        AgentSuggestionType.REQUIREMENT,
                        AgentSuggestionStatus.APPROVED
                )
                .stream()
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AgentSuggestionResponse> findRoomContractReferences(UUID userId, UUID roomId) {
        projectMembershipPublicService.assertActiveMember(userId, roomId);
        return agentSuggestionRepository.findAllByRoomIdAndSuggestionTypeInAndStatusOrderByCreatedAtDesc(
                        roomId,
                        CONTRACT_REFERENCE_TYPES,
                        AgentSuggestionStatus.APPROVED
                )
                .stream()
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneratedDocumentExportResult exportDocumentDraft(UUID userId, UUID suggestionId) {
        AgentSuggestion suggestion = agentSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_002));
        validateAccess(userId, suggestion);
        if (suggestion.getSuggestionType() != AgentSuggestionType.DOCUMENT_DRAFT) {
            throw new BusinessException(ErrorCode.AGENT_400_001);
        }
        Map<String, Object> payload = suggestion.getPayloadJson();
        return GeneratedDocumentExportResult.markdown(title(payload), contentMarkdown(payload));
    }

    private void validateAccess(UUID userId, AgentSuggestion suggestion) {
        if (suggestion.getRoomId() != null) {
            projectMembershipPublicService.assertActiveMember(userId, suggestion.getRoomId());
            return;
        }
        if (!suggestion.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.AGENT_404_002);
        }
    }

    private String title(Map<String, Object> payload) {
        String title = text(payload.get("title"));
        return title == null ? "document-draft" : title;
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
        if (content != null) {
            return content;
        }
        throw new BusinessException(ErrorCode.AGENT_400_001);
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
