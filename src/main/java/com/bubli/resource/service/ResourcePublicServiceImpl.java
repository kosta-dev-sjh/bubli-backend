package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.ResourceSummaryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}
