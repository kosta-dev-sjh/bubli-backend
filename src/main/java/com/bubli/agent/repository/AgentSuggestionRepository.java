package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentSuggestionRepository extends JpaRepository<AgentSuggestion, UUID> {

    List<AgentSuggestion> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AgentSuggestion> findAllByRoomIdOrderByCreatedAtDesc(UUID roomId);

    List<AgentSuggestion> findAllByRoomIdAndStatusOrderByCreatedAtDesc(
            UUID roomId,
            AgentSuggestionStatus status
    );

    List<AgentSuggestion> findAllByJobIdOrderByCreatedAtAsc(UUID jobId);

    List<AgentSuggestion> findAllByRoomIdAndSuggestionTypeAndStatus(
            UUID roomId,
            AgentSuggestionType suggestionType,
            AgentSuggestionStatus status
    );
}
