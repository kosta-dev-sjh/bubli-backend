package com.bubli.resource.dto;

import com.bubli.agent.dto.AgentJobTicket;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.resource.entity.Resource;

import java.util.UUID;

public record ContractDocumentUploadResponse(
        UUID resourceId,
        UUID jobId,
        AgentJobStatus status,
        boolean autoAnalyze
) {

    public static ContractDocumentUploadResponse of(Resource resource, AgentJobTicket job, boolean autoAnalyze) {
        return new ContractDocumentUploadResponse(
                resource.getId(),
                job == null ? null : job.jobId(),
                job == null ? null : job.status(),
                autoAnalyze
        );
    }
}
