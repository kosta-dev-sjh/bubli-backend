/**
 * [resource] 자료 도메인.
 *
 * 책임:
 * - 개인 자료 업로드 (visibility=PERSONAL, owner만 접근)
 * - 프로젝트룸 자료 업로드 (visibility=ROOM_SHARED, room_members 접근)
 * - 자료 목록/상세 조회
 * - 파일 메타데이터 관리 (storage_key, mime_type, size_bytes, checksum)
 * - 자료 버전 관리 (재업로드 시 새 버전 생성)
 * - 자료 댓글 (프로젝트룸 자료에만 적용)
 * - 다운로드 권한 검증 후 URL 발급
 * - 텍스트 추출 요청 (PDFBox, POI, Tika)
 * - 자료 분석 상태 관리 (UPLOADING→READY→ANALYZING→ANALYZED/FAILED)
 * - 개인 자료 → 프로젝트룸 공유 (resource_share_events)
 *
 * 엔티티:
 * - Resource         : 자료 카드 (owner_id, room_id, visibility, status)
 * - ResourceVersion  : 버전 기록 (resource_id, version_no, file_id)
 * - ResourceComment  : 자료 댓글 (resource_id, author_id, body, parent_id)
 * - ResourceAnalysis : 분석 결과 (summary_json, checklist_json, status)
 * - Document         : RAG 처리 대상 문서와 처리 상태, 버전
 * - DocumentChunk    : 페이지와 절 정보를 보존한 검색 단위
 *
 * Resource는 사용자 화면과 업무 기준의 자료 원본이고,
 * Document는 해당 자료를 RAG에서 추출·인덱싱하기 위한 처리 기록이다.
 */
package com.bubli.resource;
