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
 * - contract   : 버전별 에이전트 입출력 JSON 계약
 * - validation : 계약 구조와 제안 유형별 필수값 검증
 * - job        : 비동기 에이전트 작업 실행, 상태 추적
 * - suggestion : 후보 데이터 저장/조회/상태 관리
 * - prompt     : 프롬프트 템플릿 관리
 * - rag        : RAG 파이프라인, 임베딩, pgvector 검색 및 연결 Smoke Test
 * - model      : LLM 모델 호출, Structured Output 파싱
 *
 * 공개 Service (다른 도메인이 호출):
 * - AgentRequestService     : 자료 분석 요청 (resource.service → agent)
 * - AgentCommandService     : 채팅 명령어 처리 (chat.service → agent)
 * - AgentSuggestionService  : 후보 조회/승인 상태 (work.service → agent)
 * - PersonalAgentService    : 개인 에이전트 제안 (personal.service → agent)
 *
 * 엔티티:
 * - AgentRequest      : 비동기 AI 요청과 재시도 상태
 * - AgentSuggestion   : 원본 JSON과 사용자 수정 JSON을 보존하는 후보
 * - SuggestionEvidence: 후보와 원문 문서/청크/페이지의 근거 연결
 * - AgentModelCallLog : 모델 호출 로그 (model_id, input_tokens, output_tokens, cost)
 *
 * 금지:
 * - tasks, wbs_items, schedules 확정 데이터 직접 생성
 * - 다른 도메인 Repository/Entity 직접 사용
 * - 파일 삭제, 공유, 외부 전송, 결제, 법률 판단 확정
 */
package com.bubli.agent;
