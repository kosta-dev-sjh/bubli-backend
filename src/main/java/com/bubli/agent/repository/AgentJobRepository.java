package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentJobRepository extends JpaRepository<AgentJob, UUID> {

    @Query(value = """
            SELECT 1
            FROM (
                SELECT pg_advisory_xact_lock(
                    hashtextextended(CAST(:scopeKey AS text), CAST(0 AS bigint))
                )
            ) AS acquired
            """, nativeQuery = true)
    int acquireIdempotencyScopeLock(@Param("scopeKey") String scopeKey);

    List<AgentJob> findTop20ByJobTypeAndStatusOrderByCreatedAtAsc(
            AgentJobType jobType,
            AgentJobStatus status
    );

    Optional<AgentJob> findByIdAndRequestedByUserId(UUID id, UUID requestedByUserId);

    Optional<AgentJob> findByRequestedByUserIdAndJobTypeAndIdempotencyKey(
            UUID requestedByUserId,
            AgentJobType jobType,
            String idempotencyKey
    );

    Page<AgentJob> findByStatusAndRetryCountLessThan(
            AgentJobStatus status,
            int retryCount,
            Pageable pageable
    );
}
