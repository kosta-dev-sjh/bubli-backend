package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentJobEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentJobEventRepository extends JpaRepository<AgentJobEvent, UUID> {

	Page<AgentJobEvent> findByJobId(UUID jobId, Pageable pageable);
}
