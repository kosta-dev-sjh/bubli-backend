package com.bubli.agent.service;

import com.bubli.agent.contract.v1.AgentAnalysisResult;
import com.bubli.agent.contract.v1.Suggestion;
import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionReviewAction;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.project.service.ProjectMembershipPublicService;
import com.bubli.project.service.ProjectRoomEventPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionCommandService {

    private final AgentSuggestionRepository agentSuggestionRepository;
    private final ProjectMembershipPublicService projectMembershipPublicService;
    private final AgentSuggestionDomainApplyService agentSuggestionDomainApplyService;
    private final ProjectRoomEventPublicService projectRoomEventPublicService;

    @Transactional
    public AgentSuggestionResponse review(
            UUID suggestionId,
            UUID reviewerId,
            AgentSuggestionReviewAction action,
            Map<String, Object> payloadJson
    ) {
        AgentSuggestion suggestion = agentSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AGENT_404_002));
        validateSuggestionAccess(reviewerId, suggestion);

        try {
            switch (action) {
                case APPROVE -> {
                    suggestion.approve(reviewerId);
                    agentSuggestionDomainApplyService.applyApprovedSuggestion(reviewerId, suggestion);
                }
                case EDIT -> suggestion.edit(reviewerId, requirePayload(payloadJson));
                case HOLD -> suggestion.hold(reviewerId);
                case REJECT -> suggestion.reject(reviewerId);
                case DELETE -> {
                    AgentSuggestionResponse response = AgentSuggestionResponse.from(suggestion);
                    agentSuggestionRepository.delete(suggestion);
                    recordReviewEvent(reviewerId, suggestion, action);
                    return response;
                }
                case MODIFY -> suggestion.modify(reviewerId, requirePayload(payloadJson));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new BusinessException(ErrorCode.COMMON_400_002);
        }

        AgentSuggestionResponse response = AgentSuggestionResponse.from(suggestion);
        recordReviewEvent(reviewerId, suggestion, action);
        return response;
    }

    @Transactional
    public List<AgentSuggestionResponse> createDrafts(
            UUID userId,
            UUID roomId,
            UUID jobId,
            UUID resourceId,
            AgentAnalysisResult result
    ) {
        List<AgentSuggestion> suggestions = result.suggestions()
                .stream()
                .map(suggestion -> AgentSuggestion.draft(
                        userId,
                        roomId,
                        jobId,
                        resourceId,
                        mapType(suggestion),
                        payload(suggestion),
                        evidence(result, suggestion)
                ))
                .toList();

        return agentSuggestionRepository.saveAll(suggestions)
                .stream()
                .map(AgentSuggestionResponse::from)
                .toList();
    }

    @Transactional
    public AgentSuggestionResponse createDraft(
            UUID userId,
            UUID roomId,
            UUID jobId,
            UUID resourceId,
            AgentSuggestionType suggestionType,
            Map<String, Object> payload,
            Map<String, Object> evidence
    ) {
        AgentSuggestion suggestion = AgentSuggestion.draft(
                userId,
                roomId,
                jobId,
                resourceId,
                suggestionType,
                payload,
                evidence
        );
        return AgentSuggestionResponse.from(agentSuggestionRepository.save(suggestion));
    }

    private Map<String, Object> requirePayload(Map<String, Object> payloadJson) {
        if (payloadJson == null) {
            throw new IllegalArgumentException("editedContent is required.");
        }
        return payloadJson;
    }

    private void validateSuggestionAccess(UUID userId, AgentSuggestion suggestion) {
        if (suggestion.getRoomId() != null) {
            projectMembershipPublicService.assertActiveMember(userId, suggestion.getRoomId());
            return;
        }
        if (!suggestion.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.AGENT_404_002);
        }
    }

    private void recordReviewEvent(
            UUID reviewerId,
            AgentSuggestion suggestion,
            AgentSuggestionReviewAction action
    ) {
        if (suggestion.getRoomId() == null) {
            return;
        }
        projectRoomEventPublicService.recordAgentSuggestionReviewed(
                reviewerId,
                suggestion.getRoomId(),
                suggestion.getId(),
                suggestion.getSuggestionType().name(),
                suggestion.getStatus().name(),
                action.name()
        );
    }

    private AgentSuggestionType mapType(Suggestion suggestion) {
        return switch (suggestion.type()) {
            case TASK -> AgentSuggestionType.TASK;
            case REQUIREMENT -> AgentSuggestionType.REQUIREMENT;
            case WBS -> AgentSuggestionType.WBS;
            case QUESTION -> AgentSuggestionType.QUESTION;
            case CONTRACT_FIELD -> AgentSuggestionType.CONTRACT_FIELD;
            case REVIEW_ITEM -> AgentSuggestionType.REVIEW_ITEM;
            case DOCUMENT_DRAFT -> AgentSuggestionType.DOCUMENT_DRAFT;
            case DAILY_SUMMARY -> AgentSuggestionType.DAILY_SUMMARY;
        };
    }

    private Map<String, Object> payload(Suggestion suggestion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", suggestion.type().name());
        payload.put("title", suggestion.title());
        payload.put("description", suggestion.description());
        payload.put("fieldKey", suggestion.fieldKey());
        payload.put("value", suggestion.value());
        payload.put("confidence", suggestion.confidence());
        return payload;
    }

    private Map<String, Object> evidence(AgentAnalysisResult result, Suggestion suggestion) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("resourceId", result.resourceId().toString());
        evidence.put("sourceText", suggestion.sourceText());
        evidence.put("modelName", result.model().name());
        evidence.put("promptVersion", result.model().promptVersion());
        return evidence;
    }
}
