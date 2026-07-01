package com.bubli.user.service;

import com.bubli.user.dto.UpsertGoogleUserCommand;
import com.bubli.user.dto.UserResult;
import com.bubli.user.type.ConsentType;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.UUID;

public interface UserPublicService {

	UserResult getUser(UUID userId);

	UserResult upsertGoogleUser(UpsertGoogleUserCommand command);

	Map<UUID, UserResult> getUsers(Page<UUID> userIds);

	void assertExists(UUID userId);

	boolean isPrivacyConsentEnabled(UUID userId, ConsentType consentType);
}
