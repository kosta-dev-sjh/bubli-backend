package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentJobRepository extends JpaRepository<AgentJob, UUID> {

    List<AgentJob> findTop20ByJobTypeAndStatusOrderByCreatedAtAsc(
            AgentJobType jobType,
            AgentJobStatus status
    );

    Optional<AgentJob> findByIdAndRequestedByUserId(UUID id, UUID requestedByUserId);

    Page<AgentJob> findByStatusAndRetryCountLessThan(
            AgentJobStatus status,
            int retryCount,
            Pageable pageable
    );
}
