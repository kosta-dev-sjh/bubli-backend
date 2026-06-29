package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentDispatchOutboxRepository extends JpaRepository<AgentDispatchOutbox, UUID> {

	Optional<AgentDispatchOutbox> findByJobId(UUID jobId);

	Page<AgentDispatchOutbox> findByStatus(AgentDispatchOutboxStatus status, Pageable pageable);
}
