package com.bubli.resource.dto;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.resource.entity.Resource;

import java.util.UUID;

public record ContractDocumentUploadResponse(
        UUID resourceId,
        UUID jobId,
        AgentJobStatus status
) {

    public static ContractDocumentUploadResponse of(Resource resource, AgentJobTicket job) {
        return new ContractDocumentUploadResponse(
                resource.getId(),
                job.jobId(),
                job.status()
        );
    }
}
