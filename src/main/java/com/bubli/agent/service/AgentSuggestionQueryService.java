package com.bubli.agent.service;

import com.bubli.agent.dto.AgentSuggestionResponse;
import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.repository.AgentSuggestionRepository;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentSuggestionQueryService {

    private final AgentSuggestionRepository agentSuggestionRepository;
    private final ProjectMembershipPublicService projectMembershipPublicService;

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
}
