/**
 * [auth] 인증 도메인.
 *
 * 책임:
 * - 이메일/비밀번호 회원가입
 * - 로그인 및 JWT 액세스/리프레시 토큰 발급
 * - 토큰 만료 처리 및 재발급
 * - 비밀번호 암호화 (BCrypt)
 *
 * 주요 클래스:
 * - AuthController       : POST /api/auth/signup, /api/auth/login, /api/auth/refresh
 * - AuthService          : 가입/로그인/토큰 발급 비즈니스 로직
 * - RefreshToken(entity) : 리프레시 토큰 저장 (선택)
 * - SignupRequest(dto)   : 회원가입 요청
 * - LoginRequest(dto)    : 로그인 요청
 * - TokenResponse(dto)   : 액세스/리프레시 토큰 응답
 *
 * 금지: 프로젝트룸, 자료, 작업 도메인 로직을 직접 처리하지 않는다.
 */
package com.bubli.auth;
