package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentSuggestion;
import com.bubli.agent.type.AgentSuggestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentSuggestionRepository extends JpaRepository<AgentSuggestion, UUID> {

	Page<AgentSuggestion> findByUserIdAndStatus(UUID userId, AgentSuggestionStatus status, Pageable pageable);

	Page<AgentSuggestion> findByRoomIdAndStatus(UUID roomId, AgentSuggestionStatus status, Pageable pageable);

	List<AgentSuggestion> findByJobId(UUID jobId);
}
