# RAG/Agent Completion Roadmap

이 문서는 현재 코드 이후 **전체 Agent/RAG 기능을 제품 완성 수준까지 만들기 위해 남은 작업**을 정리한다.

기준 문서:

- `docs/09_Data-Model.md`
- `docs/10_API-Design.md`
- `docs/RAG_STEP_5_6_FLOW.md`

## 현재 완료된 기반

API surface:

- `POST /api/ai/analyze-resource`
- `POST /api/ai/generate-requirements`
- `POST /api/ai/generate-tasks`
- `POST /api/ai/generate-wbs`
- `POST /api/ai/generate-questions`
- `POST /api/ai/review-contract-documents`
- `POST /api/ai/summarize-day`
- `POST /api/ai/draft-document`
- `POST /api/ai/search-resource`
- `GET /api/agent-jobs/{jobId}`
- `GET /api/agent-jobs/{jobId}/events`
- `GET /api/agent/suggestions`
- `GET /api/project-rooms/{roomId}/agent/suggestions`
- `PATCH /api/agent/suggestions/{suggestionId}`
- `GET /api/project-rooms/{roomId}/ai-documents`
- `GET /api/resources/{resourceId}/ai-document`
- `GET /api/resources/{resourceId}/related`
- `GET /api/daily-summaries`
- `PATCH /api/daily-summaries/{id}`

구현된 기반:

- PDF chunk page number 추적
- PERSONAL / ROOM_SHARED semantic search
- search room membership 검증
- resource embedding vector formatter 공통화
- agent job dispatch/outbox/worker 기본 구조
- agent job event 조회
- agent suggestion 저장/조회/검토 상태 변경
- room suggestion 조회 membership 검증
- suggestion `APPROVE`, `EDIT`, `HOLD`, `REJECT`, `DELETE`
- local deterministic agent job execution port
- `ANALYZE_RESOURCE`의 summary, embedding, AI document 저장 흐름
- `GENERATE_*`, `DRAFT_DOCUMENT`, `DAILY_SUMMARY` job의 suggestion draft 생성
- 승인 suggestion의 `TASK`, `WBS`, `SCHEDULE` 도메인 1차 반영
- daily summary 조회/수정/승인 API
- ArchitectureTest, 주요 agent dispatch/service test 통과 확인

## 아직 제품 완성이 아닌 이유

현재 구현은 백엔드 skeleton과 1차 실행 흐름을 갖춘 상태다. 다음 항목은 아직 제품 완성 기준으로 남아 있다.

- 외부 LLM 기반 고품질 생성이 아니다. 현재 생성 job은 local deterministic payload를 만든다.
- `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY` 승인 후 전용 원본 도메인 반영 정책이 완성되지 않았다.
- `resource_relations` 자동 생성이 없다.
- daily summary는 API는 있으나 실제 활동/일정/작업/메모 기반 생성 로직은 없다.
- draft document는 suggestion으로 생성되지만 최종 문서 저장/확정 정책은 없다.
- 운영 worker/queue 실행 방식과 Postman E2E가 아직 정리되지 않았다.
- 전체 `gradlew test` 최종 통과는 아직 별도 확인이 필요하다.

## 남은 구현 순서

권장 순서:

1. `R-7` Resource relation 자동 생성
2. `R-8` 외부 LLM 기반 execution 고도화
3. `R-9` 승인 suggestion의 전용 도메인 반영 확장
4. `R-10` daily summary 실제 생성
5. `R-11` draft document 저장/확정 흐름
6. `R-12` 운영 worker/queue 실행 검증
7. `R-13` Postman E2E 문서화
8. `R-14` 전체 테스트와 schema 정합성 마무리

## R-1: Agent Job Event 조회 API

상태: 구현 완료

구현된 작업:

1. `GET /api/agent-jobs/{jobId}/events` endpoint 추가
2. `AgentJobService.getAccessibleJobEvents(...)` 추가
3. job 요청자 또는 같은 room member만 조회 가능하도록 검증
4. service test 추가

완료 기준:

- 이벤트가 생성 시간순으로 반환된다.
- 권한 없는 사용자는 event를 조회할 수 없다.

## R-2: Room Suggestion Membership 검증

상태: 구현 완료

구현된 작업:

1. room suggestion 조회에 `@CurrentUser AuthUser` 추가
2. `AgentSuggestionQueryService.findRoomSuggestions(...)`에서 membership 검증
3. agent 도메인이 project repository/entity를 직접 참조하지 않음

완료 기준:

- room member만 room suggestion을 조회할 수 있다.

## R-3: Suggestion Review API 계약 정렬

상태: 구현 완료

구현된 작업:

1. `APPROVE`, `EDIT`, `HOLD`, `REJECT`, `DELETE` 지원
2. `editedContent` 지원
3. 기존 호환용 `MODIFY`, `payloadJson` 임시 유지
4. `DELETE`는 physical delete
5. room suggestion 검토 시 membership 검증

완료 기준:

- 설계서 기준 action이 모두 동작한다.
- `EDIT` payload 누락은 400으로 실패한다.
- 최종 처리된 suggestion은 중복 변경할 수 없다.

## R-4: 승인 Suggestion의 기본 도메인 반영

상태: 1차 구현 완료

구현된 작업:

1. `AgentSuggestionDomainApplyService` 추가
2. `TASK` 승인 시 room task 생성
3. `WBS` 승인 시 `wbs_items` 생성
4. `SCHEDULE` 승인 시 schedule 생성
5. work repository/entity 직접 참조 없이 public service 경유
6. 도메인 반영 실패 시 transaction rollback

남은 것:

- 생성된 원본 도메인 id를 suggestion과 명시적으로 매핑하는 정책
- 승인 취소/삭제 시 원본 도메인 처리 정책
- `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY` 전용 반영

## R-5: Agent Job Execution 1차 구현

상태: 1차 구현 완료

구현된 작업:

1. `LocalAgentJobExecutionPort` 추가
2. `NoopAgentJobExecutionPort`는 `agent.execution.mode=noop`일 때만 활성화
3. `ANALYZE_RESOURCE`는 `ResourceAnalysisPublicService.analyzeResourceForJob(...)` 호출
4. 생성 계열 job은 suggestion draft를 생성
5. model call log를 local deterministic 값으로 기록

남은 것:

- 외부 LLM 호출
- prompt/schema version 관리
- 입력 context 수집
- output validation
- retryable/non-retryable error 분류
- token/cost logging

## R-6: 누락 API 추가

상태: 구현 완료

구현된 작업:

1. `POST /api/ai/draft-document`
2. `POST /api/ai/summarize-day`
3. `GET /api/daily-summaries`
4. `PATCH /api/daily-summaries/{id}`

남은 것:

- API가 실제 고품질 AI 결과와 연결되는지 E2E 검증
- OpenAPI/Postman 예시 정리

## R-7: Resource Relation 자동 생성

상태: 구현 완료

목표:

- `resource_relations`를 분석 결과 기반으로 자동 생성한다.
- `GET /api/resources/{resourceId}/related`가 실제 관계 데이터를 반환하게 한다.

구현 작업:

1. 분석 완료 시 같은 room의 다른 resource embedding 후보 조회
2. embedding similarity 기반 score 계산
3. relation type 정책 정의
   - 예: `SIMILAR_CONTENT`, `SAME_REQUIREMENT`, `REFERENCED_BY`, `CONTRACT_RELATED`
4. relation evidence JSON 정의
5. 중복 relation 방지
6. 권한 없는 resource가 relation 후보에 포함되지 않도록 검증
7. relation 생성/조회 test 추가

완료 기준:

- resource 분석 이후 related API에서 관계 데이터가 반환된다.
- 같은 room 또는 접근 가능한 scope 안의 resource만 relation에 포함된다.
- relation score와 evidence를 확인할 수 있다.

## R-8: 외부 LLM 기반 Execution 고도화

상태: 구현 완료

목표:

- local deterministic execution을 실제 LLM 기반 생성으로 교체 또는 확장한다.

구현 작업:

1. job type별 prompt template 정의
2. job type별 input context 수집
   - resource summary
   - semantic search hit
   - room tasks/wbs/schedules
   - chat/memory summary
3. `AiCallExecutor` 또는 별도 model gateway와 연결
4. JSON schema 기반 output validation
5. invalid output 재시도 정책
6. model call log에 promptVersion, schemaVersion, modelName, latency, token 수 기록
7. error code 분류
   - provider unavailable
   - invalid output
   - context too large
   - permission/resource missing
8. 테스트용 fake model adapter 추가

완료 기준:

- 각 job type이 실제 LLM 결과를 기반으로 suggestion/document/summary를 생성한다.
- invalid output은 실패 또는 재시도 정책에 따라 처리된다.
- model call log로 비용/성능 추적이 가능하다.

## R-9: 승인 Suggestion 전용 도메인 반영 확장

상태: 구현 완료

현재 구현:

- `TASK`
- `WBS`
- `SCHEDULE`
- `DAILY_SUMMARY`
- 그 외 보존형 suggestion type의 승인 정책

보존 정책:

1. `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`, `MEMO`는 별도 원본 도메인이 확정되지 않았으므로 승인된 suggestion 자체를 확정 결과로 보존한다.
2. `DAILY_SUMMARY`는 승인 시 `daily_summaries`에 draft/upsert로 반영한다.
3. 알 수 없는 suggestion type은 승인 반영 단계에서 `AGENT_400_001`로 거부한다.

공통 구현 작업:

1. suggestion type별 apply 정책 분리
2. domain apply 실패 시 transaction rollback
3. 승인 전 room membership/personal ownership 검증
4. JSON payload를 Map으로 저장해 승인 반영 로직이 필드를 직접 읽도록 정렬

완료 기준:

- 모든 suggestion type의 승인 결과가 명확한 원본 도메인 또는 보존 정책을 가진다.
- 승인 결과를 API로 다시 조회할 수 있다.

## R-10: Daily Summary 실제 생성

상태: 구현 완료

목표:

- `POST /api/ai/summarize-day`가 사용자의 하루 데이터를 기반으로 실제 daily summary를 만든다.

구현 작업:

1. summary date 입력 정책 확정
   - request의 `summaryDate`
   - 없으면 사용자 timezone 기준 today
2. 입력 context 수집
   - 개인 tasks
   - room tasks
   - schedules
   - memos
   - time logs
   - notifications 또는 activity event
3. LLM summary output schema 정의
4. `daily_summaries` upsert
5. 같은 날짜 중복 생성 정책
6. 승인/수정/보류 UX와 연결
7. privacy 정책
   - 개인 에이전트 원문 저장 금지
   - 저장되는 것은 요약 결과와 최소 evidence

완료 기준:

- 하루정리 job 완료 후 `GET /api/daily-summaries`에서 결과를 조회할 수 있다.
- 같은 날짜 재생성 정책이 명확하다.
- 승인/수정/보류가 동작한다.

## R-11: Draft Document 저장/확정 흐름

상태: 구현 완료

목표:

- `POST /api/ai/draft-document` 결과가 단순 suggestion을 넘어 문서 초안으로 관리된다.

구현 작업:

1. draft document request 계약 확장
   - roomId
   - documentType
   - source resource ids
   - instruction
2. LLM output schema 정의
3. `DOCUMENT_DRAFT` suggestion payload 표준화
4. 승인 시 저장 위치 결정
   - `ai_documents`
   - resource version
   - 별도 generated document 테이블
5. 문서 수정/재생성 정책
6. 다운로드/export 정책

완료 기준:

- 문서 초안을 생성, 조회, 승인, 확정 저장할 수 있다.
- 확정된 문서가 이후 AI 문서 목록 또는 resource 흐름에서 추적된다.

## R-12: 운영 Worker/Queue 실행 검증

상태: 구현 완료

현재 구현:

- dispatch/outbox/worker 구조
- in-memory/redis adapter skeleton
- retry scheduler 구조

구현 작업:

1. local/dev 실행 모드 정의
2. 운영 Redis queue 설정 검증
3. outbox scheduler 운영 설정 검증
4. worker concurrency 정책
5. job timeout 정책
6. retry backoff 정책
7. graceful shutdown 처리
8. duplicate execution 방지
9. monitoring log/metric 추가

완료 기준:

- dev/local에서 Postman으로 job 생성 후 worker가 실제 처리한다.
- 운영 profile에서 queue/outbox/worker가 안정적으로 동작한다.
- 실패/재시도/중복 실행이 통제된다.

## R-13: Postman E2E 문서화

상태: 문서화 완료

필수 시나리오:

1. access token 확보
2. project room 생성
3. room member 준비
4. PDF/TXT resource 업로드
5. `POST /api/ai/analyze-resource`
6. worker 실행
7. `GET /api/agent-jobs/{jobId}`
8. `GET /api/agent-jobs/{jobId}/events`
9. `POST /api/ai/search-resource`
10. `POST /api/ai/generate-tasks`
11. `GET /api/project-rooms/{roomId}/agent/suggestions`
12. suggestion `EDIT`
13. suggestion `APPROVE`
14. 생성된 task/wbs/schedule 조회
15. `POST /api/ai/draft-document`
16. `POST /api/ai/summarize-day`
17. `GET /api/daily-summaries`
18. 권한 실패 케이스 검증

완료 기준:

- 신규 개발자가 문서만 보고 local/dev 환경에서 E2E를 재현할 수 있다.
- happy path와 failure path가 모두 있다.

## R-14: 전체 테스트와 Schema 정합성 마무리

상태: 검증 완료

필수 테스트:

```powershell
.\gradlew.bat test --tests com.bubli.architecture.ArchitectureTest --tests com.bubli.architecture.DomainDependencyArchitectureTest --console=plain
.\gradlew.bat test --tests com.bubli.schema.EntityFlywayAlignmentTest --tests com.bubli.EntityMappingTest --console=plain
.\gradlew.bat test --tests "com.bubli.agent.dispatch.*" --console=plain
.\gradlew.bat test --console=plain
```

확인 항목:

- migration version 중복 없음
- entity field와 DB column 불일치 없음
- `ADD COLUMN IF NOT EXISTS` 파싱 문제 없음
- agent/resource/work/memory 도메인 간 repository 직접 참조 없음
- controller endpoint 중복 mapping 없음
- 권한 검증 누락 없음
- full test 통과

완료 기준:

- 전체 `gradlew test`가 통과한다.
- Postman E2E 흐름이 통과한다.
- 이 문서와 `docs/RAG_STEP_5_6_FLOW.md`가 현재 코드 상태와 일치한다.

2026-06-30 검증 결과:

```powershell
.\gradlew.bat test --tests com.bubli.architecture.ArchitectureTest --tests com.bubli.architecture.DomainDependencyArchitectureTest --tests com.bubli.schema.EntityFlywayAlignmentTest --tests com.bubli.EntityMappingTest --tests com.bubli.agent.dispatch.* --tests com.bubli.agent.service.* --tests com.bubli.resource.service.ResourceRelationIndexPublicServiceTest --tests com.bubli.memory.service.DailySummaryServiceTest --console=plain
.\gradlew.bat test --console=plain
```

두 명령 모두 통과했다.

## 최종 완료 정의

아래 조건이 모두 만족되면 **전체 Agent/RAG 기능 완료**로 본다.

1. 설계서의 Agent/RAG API가 모두 구현되어 있다.
2. 모든 Agent/RAG API에 user/room 권한 검증이 적용되어 있다.
3. resource 분석이 summary, AI document, embedding, relation을 생성한다.
4. semantic search가 PERSONAL/ROOM_SHARED에서 정확히 동작한다.
5. agent job이 운영 worker/queue에서 안정적으로 실행된다.
6. 외부 LLM 기반으로 요구사항, TODO, WBS, 질문, 문서 검토, 문서 초안, 하루정리를 생성한다.
7. LLM output은 schema validation을 통과하고 실패/재시도 정책이 있다.
8. model call log로 latency, token, model, promptVersion, schemaVersion을 추적할 수 있다.
9. 모든 suggestion type의 승인 결과가 명확한 도메인 반영 또는 보존 정책을 가진다.
10. 승인된 `TASK`, `WBS`, `SCHEDULE`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY` 결과를 API로 조회할 수 있다.
11. daily summary가 실제 사용자 활동 데이터를 기반으로 생성된다.
12. draft document가 생성, 수정, 승인, 확정 저장된다.
13. resource relation이 분석 결과 기반으로 자동 생성된다.
14. Postman E2E happy path와 failure path가 문서화되어 있다.
15. 전체 `gradlew test`가 통과한다.
