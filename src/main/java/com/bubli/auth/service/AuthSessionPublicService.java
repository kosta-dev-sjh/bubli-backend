package com.bubli.auth.service;

import java.util.UUID;

public interface AuthSessionPublicService {

	void revokeAllUserSessions(UUID userId);
}
