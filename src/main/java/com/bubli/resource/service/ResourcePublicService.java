package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceResult;

import java.util.UUID;

public interface ResourcePublicService {

	void assertReadable(UUID userId, UUID resourceId);

	ResourceResult getReadableResource(UUID userId, UUID resourceId);
}
