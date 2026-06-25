package com.bubli.resource.service;

import java.util.UUID;

public interface ResourcePublicService {

	void assertReadable(UUID userId, UUID resourceId);
}
