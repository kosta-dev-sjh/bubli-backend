package com.bubli.resource.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourcePublicService {

	private final ResourceService resourceService;

	@Transactional(readOnly = true)
	public void assertReadable(UUID userId, UUID resourceId) {
		resourceService.getResource(userId, resourceId);
	}
}
