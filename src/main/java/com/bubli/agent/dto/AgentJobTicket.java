package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;

import java.util.UUID;

public record AgentJobTicket(
        UUID jobId,
        AgentJobStatus status
) {

    public static AgentJobTicket from(AgentJob job) {
        return new AgentJobTicket(job.getId(), job.getStatus());
    }
}
