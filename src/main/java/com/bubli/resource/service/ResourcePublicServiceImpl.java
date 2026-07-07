package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceExtractedTextResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.dto.StoreResourceExtractedTextCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourcePublicServiceImpl implements ResourcePublicService {

	private final ResourceService resourceService;

	@Override
	@Transactional(readOnly = true)
	public void assertReadable(UUID userId, UUID resourceId) {
		resourceService.getResource(userId, resourceId);
	}

	@Override
	@Transactional(readOnly = true)
	public ResourceResult getReadableResource(UUID userId, UUID resourceId) {
		return resourceService.getResource(userId, resourceId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResourceResult> getRecentPersonalResources(UUID userId, int limit) {
		return resourceService.getRecentPersonalResources(userId, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResourceResult> getRecentRoomResources(UUID userId, UUID roomId, int limit) {
		return resourceService.getRecentRoomResources(userId, roomId, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResourceSummaryResult> getRecentRoomSummaries(UUID userId, UUID roomId, int limit) {
		return resourceService.getRecentRoomSummaries(userId, roomId, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResourceAnalysisSummaryResult> getRecentAnalysisSummaries(UUID userId, int limit) {
		return resourceService.getRecentAnalysisSummaries(userId, limit);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResourceSummaryResult> findResourceSummary(UUID userId, UUID resourceId) {
		return resourceService.findResourceSummary(userId, resourceId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> findAnalysisNotificationPreview(UUID jobId) {
		return resourceService.findAnalysisNotificationPreview(jobId);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResourceResult> findLatestRoomResource(UUID userId, UUID roomId, List<String> titleKeywords) {
		return resourceService.findLatestRoomResource(userId, roomId, titleKeywords);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ResourceResult> findLatestRoomFile(UUID userId, UUID roomId) {
		return resourceService.findLatestRoomFile(userId, roomId);
	}

	@Override
	@Transactional
	public ResourceResult createPersonalLocalFileResource(UUID userId, String title, Long sizeBytes, String mimeType) {
		return resourceService.createPersonalLocalFileResource(userId, title, sizeBytes, mimeType);
	}

	@Override
	@Transactional
	public ResourceResult updatePersonalLocalFileResource(UUID userId, UUID resourceId, String title, Long sizeBytes, String mimeType) {
		return resourceService.updatePersonalLocalFileResource(userId, resourceId, title, sizeBytes, mimeType);
	}

	@Override
	@Transactional
	public ResourceResult startAnalysis(UUID userId, UUID resourceId) {
		return resourceService.startAnalysis(userId, resourceId);
	}

	@Override
	@Transactional
	public ResourceExtractedTextResult storeExtractedText(UUID userId, StoreResourceExtractedTextCommand command) {
		return resourceService.storeExtractedText(userId, command);
	}

	@Override
	@Transactional
	public void deletePersonalResource(UUID userId, UUID resourceId) {
		resourceService.deleteResource(userId, resourceId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Instant> getUploadedAtBetween(UUID userId, Instant from, Instant to) {
		return resourceService.getCreatedAtBetween(userId, from, to);
	}
}
