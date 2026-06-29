/**
 * [resource] 자료 도메인.
 *
 * 책임:
 * - 개인 자료 업로드와 프로젝트룸 자료 업로드
 * - 자료 카드, 파일 메타데이터, 버전 기록 관리
 * - 자료 요약, AI 문서 분류, 검색 임베딩 저장
 * - 자료 댓글과 관련 자료 후보 관리
 *
 * 주요 엔티티:
 * - Resource          : 자료 카드(owner_id, room_id, visibility, status)
 * - ResourceFile      : 저장소 파일 메타데이터(storage_key, original_name, mime_type, size_bytes)
 * - ResourceVersion   : 자료 파일 버전(resource_id, version_no, file_id)
 * - ResourceSummary   : 에이전트 요약 결과(summary_json, checklist_json, job_id)
 * - AiDocument        : 자료의 AI 문서 분류(document_type, detected_confidence)
 * - ResourceEmbedding : RAG 검색 chunk와 vector(1024)
 */
package com.bubli.resource;
