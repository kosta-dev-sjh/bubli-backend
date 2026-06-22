/**
 * [global] 모든 도메인 공통 서버 기반 코드.
 * 특정 도메인 비즈니스 규칙은 여기에 넣지 않는다.
 *
 * config    - Spring 설정, CORS, WebSocket/STOMP, OpenAPI, 외부 연동 설정
 * error     - 공통 예외 클래스, 전역 예외 핸들러(@RestControllerAdvice), 에러 코드 매핑
 * response  - 공통 API 응답 래퍼 (success/data/error 구조, traceId 포함)
 * security  - Spring Security 설정, JWT 필터, 인증 사용자 조회 헬퍼
 * trace     - 요청별 traceId 생성, MDC 처리, 로그 추적 필터
 * websocket - WebSocket/STOMP 설정, 인증 인터셉터, topic 구독 권한 검증
 * util      - 도메인 의미 없는 순수 유틸 (날짜 변환, 파일 크기, MIME group 등)
 */
package com.bubli.global;
