/**
 * [agent] 에이전트 도메인.
 *
 * 책임:
 * - 에이전트 작업 요청 접수
 * - 문서 분석, 계약 검토, 요구사항/WBS/TODO/확인질문/문서초안 후보 생성
 * - 추천, 제안 성격의 후보 생성과 관리
 * - 프로젝트룸 채팅 명령어 응답 생성
 * - Structured Output 검증
 * - 모델 호출 로그 관리
 *
 * 내부 패키지 (다른 도메인은 직접 호출 금지):
 * - job        : 비동기 에이전트 작업 실행, 상태 추적
 * - suggestion : 후보 데이터 저장/조회/상태 관리
 * - prompt     : 프롬프트 템플릿 관리
 * - rag        : RAG 파이프라인, 임베딩, pgvector 검색
 * - model      : LLM 모델 호출, Structured Output 파싱
 *
 * 공개 Service (다른 도메인이 호출):
 * - AgentRequestService     : 자료 분석 요청 (resource.service → agent)
 * - AgentCommandService     : 채팅 명령어 처리 (chat.service → agent)
 * - AgentSuggestionService  : 후보 조회/승인 상태 (work.service → agent)
 * - PersonalAgentService    : 개인 에이전트 제안 (personal.service → agent)
 *
 * 엔티티:
 * - AgentJob          : 에이전트 작업 (room_id, resource_id, job_type, status)
 * - AgentJobEvent     : 작업 이벤트 로그 (agent_job_id, event_type, payload)
 * - AgentSuggestion   : 제안/후보 (user_id, room_id, suggestion_type, payload_json,
 *                        status: DRAFT/APPROVED/HELD/REJECTED)
 * - AgentModelCallLog : 모델 호출 로그 (model_id, input_tokens, output_tokens, cost)
 *
 * 금지:
 * - tasks, wbs_items, schedules 확정 데이터 직접 생성
 * - 다른 도메인 Repository/Entity 직접 사용
 * - 파일 삭제, 공유, 외부 전송, 결제, 법률 판단 확정
 */
package com.bubli.agent;
