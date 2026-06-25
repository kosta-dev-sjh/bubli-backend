package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentJobRepository extends JpaRepository<AgentJob, UUID> {

    List<AgentJob> findTop20ByJobTypeAndStatusOrderByCreatedAtAsc(
            AgentJobType jobType,
            AgentJobStatus status
    );
}
