package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourcePublicService {

	void assertReadable(UUID userId, UUID resourceId);

	ResourceResult getReadableResource(UUID userId, UUID resourceId);

	List<ResourceSummaryResult> getRecentRoomSummaries(UUID userId, UUID roomId, int limit);

	Optional<ResourceResult> findLatestRoomResource(UUID userId, UUID roomId, List<String> titleKeywords);

	Optional<ResourceResult> findLatestRoomFile(UUID userId, UUID roomId);

	List<ResourceAnalysisSummaryResult> getRecentAnalysisSummaries(UUID userId, int limit);

	ResourceResult createPersonalResource(UUID userId, String title);

	void deletePersonalResource(UUID userId, UUID resourceId);
}
