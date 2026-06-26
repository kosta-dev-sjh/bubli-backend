/**
 * [agent] 에이전트 도메인.
 *
 * 책임:
 * - agent_jobs 기반 비동기 AI 작업 생성과 상태 추적
 * - 자료 분석, 요구사항/WBS/TODO/확인 질문/문서 초안 후보 생성
 * - agent_suggestions 기반 후보 저장, 조회, 승인/보류/거절
 * - 모델 호출, structured output 파싱, RAG 파이프라인 연동
 *
 * 주요 엔티티:
 * - AgentJob        : 비동기 AI 작업(job_type, status, resource_id, room_id)
 * - AgentSuggestion : 에이전트 후보(payload_json, evidence_json, status)
 * - AgentJobEvent   : 작업 상태 전이 이벤트
 * - AgentModelCallLog : 모델 호출 로그와 비용/품질 추적
 */
package com.bubli.agent;
