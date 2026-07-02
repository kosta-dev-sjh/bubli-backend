package com.bubli.resource.service;

import com.bubli.resource.dto.CreateResourceCommand;
import com.bubli.resource.dto.ResourceAnalysisSummaryResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	public ResourceResult createPersonalResource(UUID userId, String title) {
		return resourceService.create(userId,
				new CreateResourceCommand(title, ResourceKind.FILE, ResourceVisibility.PERSONAL, null));
	}

	@Override
	@Transactional
	public ResourceResult updatePersonalResource(UUID userId, UUID resourceId, String title) {
		return resourceService.updateResource(userId, resourceId, title);
	}

	@Override
	@Transactional
	public void deletePersonalResource(UUID userId, UUID resourceId) {
		resourceService.deleteResource(userId, resourceId);
	}
}
