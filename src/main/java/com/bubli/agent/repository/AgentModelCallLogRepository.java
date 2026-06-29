package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentModelCallLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentModelCallLogRepository extends JpaRepository<AgentModelCallLog, UUID> {
}
