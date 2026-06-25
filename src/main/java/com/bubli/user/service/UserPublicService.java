package com.bubli.user.service;

import com.bubli.user.dto.UserResult;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

public interface UserPublicService {

	UserResult getUser(UUID userId);

	Map<UUID, UserResult> getUsers(Page<UUID> userIds);

	void assertExists(UUID userId);
}
