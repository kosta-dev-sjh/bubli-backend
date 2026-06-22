/**
 * [localsync] 로컬 동기화 도메인.
 *
 * 책임:
 * - Tauri 개인 관리 폴더 이벤트 수신
 * - 로컬 파일 색인 동기화 상태 관리
 * - 서버 개인 자료함 반영 (사용자 승인 건만)
 * - 동기화 대기열 처리
 * - 중복 요청 방지용 idempotency_key 검증
 *
 * 주요 엔티티 후보:
 * - ManagedFolder : 사용자가 등록한 개인 관리 폴더 (user_id, folder_name, sync_enabled)
 * - SyncJob       : 동기화 작업 단위 (user_id, status, total_count, failed_count)
 *
 * 규칙:
 * - 개인 자료를 프로젝트룸에 자동 공유하지 않는다.
 * - 사용자가 승인한 이벤트만 서버에 반영한다.
 * - idempotency_key 없이 재전송 요청을 서버에 반영하지 않는다.
 */
package com.bubli.localsync;
