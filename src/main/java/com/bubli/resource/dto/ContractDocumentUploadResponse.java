package com.bubli.resource.dto;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.resource.entity.Resource;

import java.util.UUID;

public record ContractDocumentUploadResponse(
        UUID resourceId,
        UUID jobId,
        AgentJobStatus status
) {

    public static ContractDocumentUploadResponse of(Resource resource, AgentJob job) {
        return new ContractDocumentUploadResponse(
                resource.getId(),
                job.getId(),
                job.getStatus()
        );
    }
}
