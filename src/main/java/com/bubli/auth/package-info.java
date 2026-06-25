/**
 * [auth] 인증 도메인.
 *
 * 책임:
 * - Google 로그인 사용자 확인
 * - JWT 액세스/리프레시 토큰 발급
 * - 토큰 만료 처리 및 재발급
 * - refresh token 수명 주기 관리
 *
 * 주요 클래스:
 * - AuthController       : POST /api/auth/google/login, /api/auth/refresh
 * - AuthService          : Google 로그인/토큰 발급 비즈니스 로직
 * - RefreshToken(entity) : 리프레시 토큰 저장 (선택)
 * - TokenResponse(dto)   : 액세스/리프레시 토큰 응답
 *
 * 금지: 프로젝트룸, 자료, 작업 도메인 로직을 직접 처리하지 않는다.
 */
package com.bubli.auth;
