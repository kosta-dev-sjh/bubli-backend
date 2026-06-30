# AI Final Plan Gap Implementation

## AI-1 implementation status

Status: implemented in code.

- `ANALYZE_RESOURCE` now extracts the document source, sends the text to the LLM, validates the `analysis.v1` contract, stores structured analysis in `resource_summaries.summary_json`, creates agent suggestion drafts, and marks the resource failed when model output is invalid.
- Local fallback path still works through `ResourceAnalysisPublicService.analyzeResourceForJob(...)` with `LOCAL_EXTRACTOR` summary data.
- Verified with `.\gradlew.bat test --console=plain`.

## AI-2 implementation status

Status: implemented in code.

- Added `AgentJobContextCollector` to gather bounded prompt context for LLM jobs.
- Context now includes recent room resource summaries, room tasks, room WBS items, room schedules, recent room chat, room memory summaries, requester personal tasks, and requester schedules.
- Cross-domain reads are exposed through `*PublicService` contracts to keep ArchitectureTest rules intact.
- `LlmAgentJobExecutionPort` includes the collected context block in both normal agent job prompts and `ANALYZE_RESOURCE` prompts.
- Verified with `.\gradlew.bat test --console=plain`.

## AI-3 implementation status

Status: implemented in code.

- Project room document upload now validates active room membership before storing the file.
- Upload API supports `autoAnalyze` with default `true`.
- When `autoAnalyze=true`, upload creates an `ANALYZE_RESOURCE` job through the normal agent job dispatch path and includes upload metadata in `requestPayload`.
- When `autoAnalyze=false`, the resource is stored as `READY` and no agent job is created.
- Upload response now includes `autoAnalyze`, and `jobId/status` are nullable when no job is created.
- Verified with `.\gradlew.bat test --console=plain`.

## AI-4 implementation status

Status: implemented in code.

- Approved `TASK`, `TODO`, `WBS`, `SCHEDULE`, and `DAILY_SUMMARY` suggestions are materialized through domain `*PublicService` contracts.
- Approved `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`, and `MEMO` suggestions are preserved as approved review artifacts.
- Approval now records `payloadJson.appliedResult` with target type, target id when available, and applied timestamp.
- Verified with `.\gradlew.bat test --console=plain`.

## AI-5 implementation status

Status: implemented in code.

- Added project room agent command endpoint: `POST /api/project-rooms/{roomId}/agent/commands`.
- Command supports `ANSWER`, `SUMMARIZE`, and `SUGGEST` modes.
- Command service validates active room membership, collects AI-2 context, generates an LLM response when `ai` profile ChatModel is available, and falls back to a local response otherwise.
- Agent responses are stored as `chat_messages.message_type = AGENT_RESPONSE`.
- Each command response creates a draft `room_memory_summaries` entry for later context reuse.
- Verified with `.\gradlew.bat test --console=plain`.

이 문서는 `Bubli_최종기획_완성본_v15_DB회의반영_2026-06-24.md`를 기준으로, 현재 백엔드 AI/RAG 구현에서 아직 부족한 부분을 구현 계획으로 정리한다.

제외 범위:

- API 경로명 정렬
  - 예: 기획안의 `/api/agent/jobs`와 현재 코드의 `/api/ai/*`, `/api/agent-jobs/*` 차이
  - 이 문서는 기능 구현 계획만 다룬다.

현재 구현된 기반:

- Bedrock Converse/Titan 기반 LLM/embedding 연결
- Redis queue 기반 agent job dispatch/worker
- `ANALYZE_RESOURCE`, `GENERATE_REQUIREMENTS`, `GENERATE_TASKS`, `GENERATE_WBS`, `GENERATE_QUESTIONS`, `REVIEW_CONTRACT_DOCUMENTS`, `DRAFT_DOCUMENT`, `DAILY_SUMMARY`
- `agent_jobs`, `agent_job_events`, `agent_model_call_logs`, `agent_suggestions`
- TXT/PDF 분석, `resource_summaries`, `resource_embeddings`, `resource_relations`
- PERSONAL/ROOM_SHARED semantic search
- suggestion 검토와 `TASK`, `WBS`, `SCHEDULE`, `DAILY_SUMMARY` 일부 도메인 반영
- `local,ai` profile E2E 검증

## 구현 우선순위

1. AI-1 문서 특화 분석 품질 강화
2. AI-2 agent job context 수집 강화
3. AI-3 프로젝트룸 생성 문서 업로드와 분석 연결
4. AI-4 승인 suggestion 도메인 반영 확장
5. AI-5 프로젝트룸 채팅 에이전트와 장기기억
6. AI-6 하루정리 실제 context 생성
7. AI-7 비용 제한과 분석 캐시
8. AI-8 완료 알림과 실시간 전달
9. AI-9 품질 평가 fixtures와 회귀 테스트

## AI-1 문서 특화 분석 품질 강화

기획 근거:

- FR-21: 문서 요약, 놓치면 안 되는 내용, 다시 확인할 부분 생성
- 계약서/요구사항/회의록에서 금액, 납기, 지급 조건, 수정 범위, 요구사항, 결정사항, 담당자, 날짜를 추출해야 한다.
- NFR-06: 계약 문서 분석은 확인 보조로 표시하고 확정 판단을 하지 않는다.

현재 부족한 점:

- `ANALYZE_RESOURCE`는 텍스트 추출, preview summary, embedding, AI document 분류 중심이다.
- LLM 기반 구조화 분석 결과가 `resource_summaries`와 `agent_suggestions`에 충분히 반영되지 않는다.
- 문서 유형별 추출 schema가 약하다.

구현 작업:

1. 문서 분석 output schema 정의
   - `summary`
   - `importantPoints`
   - `reviewItems`
   - `extractedFields`
   - `requirements`
   - `questions`
   - `risks`
   - `sourceEvidence`
2. 문서 유형별 prompt 분리
   - CONTRACT
   - REQUIREMENT
   - MEETING_NOTE
   - REFERENCE
3. `ANALYZE_RESOURCE`에서 LLM 분석 수행
   - 텍스트 추출 후 chunk/preview만 저장하지 않고 LLM 분석 결과 저장
   - `resource_summaries.summary_json`에 구조화 결과 저장
4. 분석 결과에서 suggestion 생성
   - `REQUIREMENT`
   - `QUESTION`
   - `REVIEW_ITEM`
   - `CONTRACT_FIELD`
   - `TASK`
   - `WBS`
5. 계약 문서 안전 문구 강제
   - 법률 판단 금지
   - 문제 확정 표현 금지
   - 확인 보조 문구 포함
6. schema validation 실패 처리
   - job 실패 또는 fallback summary 정책 결정
   - `agent_job_events`에 실패 사유 기록

완료 기준:

- 계약서 샘플 분석 시 지급 조건, 납기, 확인 질문 후보가 생성된다.
- 요구사항 샘플 분석 시 요구사항, WBS/TODO 후보가 생성된다.
- 회의록 샘플 분석 시 결정사항, 담당자, 일정/TODO 후보가 생성된다.
- 분석 결과가 자료 상세에서 조회 가능한 `resource_summaries`에 남는다.
- 계약 분석 문구가 확정 판단으로 보이지 않는다.

## AI-2 Agent Job Context 수집 강화

기획 근거:

- 프로젝트룸 에이전트는 프로젝트룸 자료, 채팅, 댓글, 버전 기록, `room_memory_summaries`를 본다.
- 개인 에이전트는 개인 자료, 접근 가능한 프로젝트룸 자료, 내 TODO, 내 메모, 내 타이머, 일정, 활동 기록을 본다.

현재 부족한 점:

- LLM prompt가 `jobType`, `roomId`, `resourceId`, `requestPayload` 중심이다.
- 실제 자료 요약, semantic search hit, room tasks/wbs/schedules, 채팅, 댓글, 장기기억을 충분히 수집하지 않는다.

구현 작업:

1. `AgentJobContextCollector` 추가
2. room job context 수집
   - 최근 resource summaries
   - semantic search topK
   - room tasks
   - room WBS
   - room schedules
   - resource comments
   - 최근 chat messages
   - room memory summaries
3. personal job context 수집
   - 개인 tasks
   - 담당자로 배정된 room tasks
   - schedules
   - notifications
   - time logs
   - daily summaries
4. context size 제한
   - 최대 문자 수
   - 항목별 topK
   - 오래된 데이터 제외
5. 권한 검증
   - room context는 active member만
   - personal context는 userId 기준
   - 개인 자료는 공유 전 room context에 포함 금지
6. prompt에 context block 추가
7. model call log에 context size 기록 검토

완료 기준:

- `GENERATE_TASKS`가 room 자료 요약과 기존 WBS/TODO를 참고한다.
- `GENERATE_QUESTIONS`가 resource summary와 review item을 참고한다.
- room member가 아닌 사용자의 context 접근이 차단된다.
- context가 길어도 job이 실패하지 않고 제한된 입력으로 수행된다.

## AI-3 프로젝트룸 생성 문서 업로드와 분석 연결

기획 근거:

- FR-03: 프로젝트룸 생성 시 계약서, 요구사항 문서, 회의록 업로드 가능
- 프로젝트룸 생성 흐름에서 문서 분석과 후보 확인이 이어져야 한다.

현재 부족한 점:

- 프로젝트룸 생성과 자료 업로드/분석이 분리되어 있다.
- 문서 업로드 후 분석 job 연결은 일부 있으나, 프로젝트룸 생성 결과로 후보 확인까지 묶이지 않는다.

구현 작업:

1. 프로젝트룸 생성 후 문서 업로드 flow 정의
   - controller는 기존 경로 유지
   - service level에서 room 생성, resource upload, analyze job 생성 연결
2. room document upload command 정리
   - document type hint
   - file
   - autoAnalyze flag
3. 업로드 완료 후 자동 `ANALYZE_RESOURCE` job 생성
4. job ticket을 응답에 포함
5. 프로젝트룸 생성 결과에서 agent job ids 반환 또는 별도 조회 제공
6. 업로드 실패, 분석 실패 분리
7. room membership 생성 시점과 분석 권한 검증 순서 정리

완료 기준:

- 프로젝트룸 생성 직후 문서 업로드와 분석 job 생성까지 한 흐름으로 검증 가능하다.
- 사용자는 job id로 분석 상태를 추적할 수 있다.
- 분석 결과 suggestion이 프로젝트룸 제안함에 표시된다.

## AI-4 승인 Suggestion 도메인 반영 확장

기획 근거:

- FR-23: APPROVED만 tasks, wbs_items, schedules 같은 확정 데이터로 반영
- 기획 정리: 승인한 것만 자료, TODO, WBS, 일정, 프로젝트룸 참고 정보와 확인 필요 항목으로 반영

현재 부족한 점:

- `TASK`, `WBS`, `SCHEDULE`, `DAILY_SUMMARY`는 반영된다.
- `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`는 승인된 suggestion으로 보존만 한다.

구현 작업:

1. suggestion type별 apply handler 분리
2. `REQUIREMENT` 반영 정책
   - 별도 requirement table이 없으면 WBS/TASK 변환 정책 정의
   - 또는 approved requirement view로 유지
3. `QUESTION` 반영 정책
   - 확인 필요 항목으로 보존
   - 알림 또는 project room event 생성 검토
4. `REVIEW_ITEM`, `CONTRACT_FIELD`, `CONTRACT_REVIEW` 반영 정책
   - resource summary review section에 연결
   - project room event 생성
   - 확정 판단 금지 문구 유지
5. `DOCUMENT_DRAFT` 반영 정책
   - `ai_documents` 또는 generated document 저장 위치 결정
   - 승인 후 조회 가능한 문서 결과 생성
6. idempotency
   - 같은 suggestion 중복 승인 시 도메인 중복 생성 방지
7. mapping 저장
   - suggestion id -> created domain id 추적
8. rollback test

완료 기준:

- 모든 suggestion type은 승인 후 명확한 결과를 가진다.
- 승인 결과를 API로 다시 조회할 수 있다.
- 중복 승인으로 같은 task/wbs/schedule/document가 중복 생성되지 않는다.

## AI-5 프로젝트룸 채팅 에이전트와 장기기억

기획 근거:

- 프로젝트룸 채팅에서 에이전트를 호출할 수 있다.
- 에이전트 응답은 `AGENT_RESPONSE` 메시지로 저장한다.
- 장기요약은 `room_memory_summaries`에 저장한다.

현재 부족한 점:

- `MessageType.AGENT_RESPONSE`, `RoomMemorySummary` 엔티티 흔적은 있으나 실제 명령 API와 장기기억 생성/조회 흐름이 부족하다.

구현 작업:

1. project room agent command service 추가
2. command 입력 계약 정의
   - roomId
   - message
   - optional resourceIds
   - mode: ANSWER, SUMMARIZE, SUGGEST
3. room membership 검증
4. context 수집
   - 최근 chat messages
   - room memory summaries
   - related resources
   - WBS/TODO/schedules
5. LLM 응답 생성
6. `chat_messages`에 `AGENT_RESPONSE` 저장
7. 필요한 경우 `agent_suggestions` 생성
8. `room_memory_summaries` 생성/조회 API 구현
9. 장기요약 갱신 정책
   - 수동 생성
   - 메시지 개수 기준
   - 시간 기준

완료 기준:

- 프로젝트룸 채팅에서 에이전트 명령을 호출할 수 있다.
- 응답이 `AGENT_RESPONSE`로 저장되고 채팅 조회에서 보인다.
- room memory summary를 생성하고 다시 context로 사용할 수 있다.

## AI-6 하루정리 실제 Context 생성

기획 근거:

- 하루 종료 또는 사용자 요청 시 완료 TODO, 남은 TODO, 일정, 알림, 작업 시간, 위젯 사용 집계, 로컬 요약을 기반으로 하루정리 후보를 만든다.

현재 부족한 점:

- `DAILY_SUMMARY` job과 `daily_summaries` upsert는 있다.
- 실제 하루 데이터 context 수집이 부족하다.

구현 작업:

1. `DailySummaryContextCollector` 추가
2. 날짜/timezone 정책 정리
   - request summaryDate
   - user timezone
3. context 수집
   - 완료 TODO
   - 남은 TODO
   - 오늘 일정
   - 읽지 않은 알림 또는 확인한 알림
   - time logs
   - widget daily summaries
   - activity logs
4. Tauri local summary 입력 계약 정의
   - 서버에 원문 대화 저장 금지
   - 사용자가 확인한 요약만 request로 전달 가능
5. LLM 하루정리 schema 정의
   - done
   - remaining
   - tomorrowFocus
   - risks
   - evidence
6. `DAILY_SUMMARY` suggestion 생성
7. 승인 시 `daily_summaries` upsert

완료 기준:

- 실제 task/schedule/time log 기반 daily summary가 생성된다.
- 같은 날짜 재생성 정책이 명확하다.
- 개인 에이전트 원문이 서버에 저장되지 않는다.

## AI-7 비용 제한과 분석 캐시

기획 근거:

- NFR-15: 모델 호출은 사용자별 하루 제한을 둔다.
- NFR-16: 같은 파일 hash는 반복 분석하지 않는다.
- 운영 계획: 사용자별 하루 모델 호출 제한, 기능별 호출 제한, 같은 파일 hash 분석 캐시.

현재 부족한 점:

- model call log는 있다.
- 사용자별/기능별 호출 제한과 file hash 기반 분석 cache는 부족하다.

구현 작업:

1. model usage policy 정의
   - user/day limit
   - jobType/day limit
   - room/day limit optional
2. `AgentModelUsageGuard` 추가
3. job 생성 전 limit 검사
4. 초과 시 business error와 event 기록
5. `agent_model_call_logs` 기반 집계 또는 별도 usage table 결정
6. file hash 분석 cache
   - resource file checksum 기준
   - 같은 checksum + same visibility scope + same analysis version이면 재사용
7. cache hit 시
   - summary 복제 또는 연결
   - embedding 재사용 가능 여부 검토
   - model call 생략
8. cache invalidation
   - prompt/schema version 변경
   - file version 변경

완료 기준:

- 사용자별 하루 호출 제한이 적용된다.
- 같은 파일을 반복 분석해도 불필요한 LLM 호출을 하지 않는다.
- 제한/캐시 여부가 job event 또는 log로 추적된다.

## AI-8 완료 알림과 실시간 전달

기획 근거:

- 에이전트 완료는 WebSocket 이벤트와 알림으로 화면에 전달한다.
- FR-33: 에이전트 정리 완료 알림.

현재 부족한 점:

- `agent_job_events`는 저장된다.
- notification 생성과 실시간 push 연결은 부족하다.

구현 작업:

1. agent job lifecycle event 발행
   - started
   - succeeded
   - failed
2. notification 생성
   - sourceType: AGENT_JOB 또는 AGENT_SUGGESTION
   - sourceId
   - title/body
3. 사용자 알림 설정 반영
4. project room event와 연결
5. WebSocket topic 발행
   - 개인 topic
   - project room topic
6. 실패 알림과 재시도 가능 상태 표시 정보 포함

완료 기준:

- 분석 완료 시 사용자가 알림 목록에서 확인할 수 있다.
- 프로젝트룸 멤버에게 필요한 범위로만 알림이 전달된다.
- 실패 job도 실패 사유와 함께 알림/이벤트로 확인된다.

## AI-9 품질 평가 Fixtures와 회귀 테스트

기획 근거:

- 에이전트 품질은 계약서, 요구사항 문서, 회의록 샘플과 기대 결과 JSON 비교로 검증한다.

현재 부족한 점:

- 단위/아키텍처 테스트는 있으나 기획 품질 기준 fixture가 부족하다.
- LLM 결과 품질을 자동으로 판정하는 기준이 약하다.

구현 작업:

1. fixture 문서 추가
   - 계약서
   - 요구사항 문서
   - 회의록
   - 충돌 조건 문서
   - 권한 범위 문서
2. expected JSON 정의
   - 최소 필수 field
   - suggestion type
   - evidence 포함 여부
3. fake model adapter 기반 deterministic 품질 테스트
4. Bedrock smoke 품질 checklist 문서화
5. Postman E2E에 품질 확인 항목 추가
6. regression test
   - schema validation
   - prompt version
   - model call log
   - permission boundary

완료 기준:

- fixture 기반 테스트로 계약서/요구사항/회의록 분석 결과의 최소 품질을 검증한다.
- prompt 변경 시 schema와 핵심 결과가 깨지는지 확인할 수 있다.
- 실제 Bedrock E2E에서 사람이 확인해야 할 품질 항목이 문서화된다.

## 최종 완료 정의

API 경로 정렬을 제외하고 아래 조건이 만족되면 기획안 기준 AI 기능 구현 완료로 본다.

1. 문서 분석이 문서 유형별 구조화 결과와 후보를 생성한다.
2. LLM job이 실제 room/personal context를 수집해서 사용한다.
3. 프로젝트룸 생성/문서 업로드/분석/후보 확인 흐름이 연결된다.
4. 모든 suggestion type의 승인 후 결과 정책이 명확하고 조회 가능하다.
5. 프로젝트룸 채팅 에이전트 응답과 장기기억이 동작한다.
6. 하루정리가 실제 사용자 작업 context 기반으로 생성된다.
7. 모델 호출 제한과 file hash 분석 캐시가 적용된다.
8. agent 완료/실패 알림과 실시간 전달이 동작한다.
9. 계약서, 요구사항, 회의록 fixture 기반 품질 회귀 테스트가 있다.
