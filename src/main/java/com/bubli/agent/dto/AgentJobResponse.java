package com.bubli.agent.dto;

import com.bubli.agent.entity.AgentJob;
import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgentJobResponse(
        UUID jobId,
        AgentJobType jobType,
        AgentJobStatus status,
        UUID resourceId,
        UUID roomId,
        String errorCode,
        String errorMessage,
        int retryCount,
        List<UUID> suggestionIds,
        UUID resourceSummaryId,
        UUID aiDocumentId,
        Instant startedAt,
        Instant finishedAt
) {

    public static AgentJobResponse of(
            AgentJob job,
            List<UUID> suggestionIds,
            UUID resourceSummaryId,
            UUID aiDocumentId
    ) {
        return new AgentJobResponse(
                job.getId(),
                job.getJobType(),
                job.getStatus(),
                job.getResourceId(),
                job.getRoomId(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getRetryCount(),
                suggestionIds,
                resourceSummaryId,
                aiDocumentId,
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }
}
