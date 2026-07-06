package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceExtractedTextResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.dto.StoreResourceExtractedTextCommand;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourcePublicService {

	void assertReadable(UUID userId, UUID resourceId);

	ResourceResult getReadableResource(UUID userId, UUID resourceId);

	List<ResourceSummaryResult> getRecentRoomSummaries(UUID userId, UUID roomId, int limit);

	List<ResourceAnalysisSummaryResult> getRecentAnalysisSummaries(UUID userId, int limit);

	Optional<ResourceSummaryResult> findResourceSummary(UUID userId, UUID resourceId);

	Optional<ResourceResult> findLatestRoomResource(UUID userId, UUID roomId, List<String> titleKeywords);

	Optional<ResourceResult> findLatestRoomFile(UUID userId, UUID roomId);

	ResourceResult createPersonalResource(UUID userId, String title);

	ResourceResult updatePersonalResource(UUID userId, UUID resourceId, String title);

	ResourceResult startAnalysis(UUID userId, UUID resourceId);

	ResourceExtractedTextResult storeExtractedText(UUID userId, StoreResourceExtractedTextCommand command);

	void deletePersonalResource(UUID userId, UUID resourceId);

	List<Instant> getUploadedAtBetween(UUID userId, Instant from, Instant to);
}
