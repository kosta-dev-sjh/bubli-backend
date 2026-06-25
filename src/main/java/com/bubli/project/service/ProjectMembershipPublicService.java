package com.bubli.project.service;

import java.util.List;
import java.util.UUID;

public interface ProjectMembershipPublicService {

	void assertActiveMember(UUID userId, UUID roomId);

	boolean isActiveMember(UUID userId, UUID roomId);

	List<UUID> findActiveRoomIds(UUID userId);

	void assertProjectLeader(UUID userId, UUID roomId);
}
