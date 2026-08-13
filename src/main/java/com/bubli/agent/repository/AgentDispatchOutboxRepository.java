package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentDispatchOutbox;
import com.bubli.agent.type.AgentDispatchOutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentDispatchOutboxRepository extends JpaRepository<AgentDispatchOutbox, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<AgentDispatchOutbox> findByJobId(UUID jobId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<AgentDispatchOutbox> findByStatus(AgentDispatchOutboxStatus status, Pageable pageable);
}
