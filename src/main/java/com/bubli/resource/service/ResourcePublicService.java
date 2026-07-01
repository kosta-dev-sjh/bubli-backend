package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;

import java.util.List;
import java.util.UUID;

public interface ResourcePublicService {

	void assertReadable(UUID userId, UUID resourceId);

	ResourceResult getReadableResource(UUID userId, UUID resourceId);

	List<ResourceSummaryResult> getRecentRoomSummaries(UUID userId, UUID roomId, int limit);

	List<ResourceAnalysisSummaryResult> getRecentAnalysisSummaries(UUID userId, int limit);
}
