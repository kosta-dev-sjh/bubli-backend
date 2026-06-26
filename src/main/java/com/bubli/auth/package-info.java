/**
 * [auth] 인증 도메인.
 *
 * 책임:
 * - Google OAuth/OIDC 로그인
 * - JWT 액세스/리프레시 토큰 발급
 * - 토큰 만료 처리 및 재발급
 * - user_sessions 기준 기기 세션 관리
 *
 * 주요 클래스:
 * - AuthController       : GET /api/auth/google/authorize, POST /api/auth/google/callback, refresh, logout
 * - AuthService          : Google OAuth code 검증, 토큰 발급, 세션 관리
 * - UserSession(entity)  : refresh token hash와 기기 세션 저장
 * - GoogleCallbackRequest(dto): Google OAuth callback 요청
 * - AuthTokenResponse(dto): 액세스/리프레시 토큰 응답
 *
 * 금지: 프로젝트룸, 자료, 작업 도메인 로직을 직접 처리하지 않는다.
 */
package com.bubli.auth;
