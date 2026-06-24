package com.bubli.auth.service;

import com.bubli.auth.dto.AuthLoginRequest;
import com.bubli.auth.dto.AuthTokenResponse;
import com.bubli.auth.dto.RefreshTokenRequest;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

	@Transactional
	public AuthTokenResponse login(AuthLoginRequest request) {
		// TODO: Google ID token 검증 후 users와 user_sessions를 생성하거나 갱신한다.
		// TODO: refresh token 원문은 저장하지 않고 hash로 저장하도록 구현한다.
		throw new BusinessException(ErrorCode.AUTH_501_001);
	}

	@Transactional
	public AuthTokenResponse refresh(RefreshTokenRequest request) {
		// TODO: refresh token rotation과 재사용 감지를 user_sessions 기준으로 구현한다.
		throw new BusinessException(ErrorCode.AUTH_501_001);
	}

	@Transactional
	public void logout(UUID userId) {
		// TODO: 현재 기기 세션의 user_sessions.status를 REVOKED로 바꾼다.
		throw new BusinessException(ErrorCode.AUTH_501_001);
	}
}
