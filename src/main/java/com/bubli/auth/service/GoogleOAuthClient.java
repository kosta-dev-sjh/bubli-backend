package com.bubli.auth.service;

import com.bubli.auth.dto.GoogleCallbackCommand;
import com.bubli.auth.dto.GoogleUserProfile;

public interface GoogleOAuthClient {
	GoogleUserProfile fetchUserProfile(GoogleCallbackCommand command);
}
