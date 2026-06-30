package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import com.bubli.agent.type.AgentSuggestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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

    List<AgentSuggestion> findAllByRoomIdAndSuggestionTypeInOrderByCreatedAtDesc(
            UUID roomId,
            Collection<AgentSuggestionType> suggestionTypes
    );

    List<AgentSuggestion> findAllByRoomIdAndSuggestionTypeInAndStatusOrderByCreatedAtDesc(
            UUID roomId,
            Collection<AgentSuggestionType> suggestionTypes,
            AgentSuggestionStatus status
    );

    Page<AgentSuggestion> findByUserIdAndStatus(
            UUID userId,
            AgentSuggestionStatus status,
            Pageable pageable
    );

    Page<AgentSuggestion> findByRoomIdAndStatus(
            UUID roomId,
            AgentSuggestionStatus status,
            Pageable pageable
    );
}
