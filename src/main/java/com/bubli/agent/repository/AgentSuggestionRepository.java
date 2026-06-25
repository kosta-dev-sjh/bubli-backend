package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentSuggestionRepository extends JpaRepository<AgentSuggestion, UUID> {

    List<AgentSuggestion> findAllByProjectRoomIdAndStatusOrderByCreatedAtDesc(
            UUID projectRoomId,
            AgentSuggestionStatus status
    );

    List<AgentSuggestion> findAllByAgentRequestIdOrderByCreatedAtAsc(UUID agentRequestId);

    List<AgentSuggestion> findAllByProjectRoomIdAndSuggestionTypeAndStatus(
            UUID projectRoomId,
            AgentSuggestionType suggestionType,
            AgentSuggestionStatus status
    );
}
