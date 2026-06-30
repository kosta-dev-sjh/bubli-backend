# AI 기능 명세서

작성일: 2026-06-30

이 문서는 현재 코드 기준으로 구현된 AI/Agent/RAG 관련 기능을 추출한 명세다. API 예제는 `docs/http/agent.http`에도 있다.

## 1. 실행 구조

### 1.1 비동기 Agent Job 흐름

대부분의 AI 생성 기능은 즉시 결과를 반환하지 않고 `agent_jobs`를 만든다.

1. 클라이언트가 `/api/ai/...` job 생성 API 호출
2. 서버가 권한 확인 후 `agent_jobs` row 생성, 상태 `PENDING`
3. dispatch adapter가 job queue로 전달
4. worker가 queue를 poll
5. execution port가 실제 처리
6. 성공 시 `agent_suggestions`, `resource_summaries`, `ai_documents`, `resource_embeddings` 등 저장
7. 클라이언트는 `/api/agent-jobs/{jobId}`와 `/api/agent-jobs/{jobId}/events`로 추적

### 1.2 실행 모드

| 모드 | 설정 | 특징 | E2E 의미 |
|---|---|---|---|
| 기본 local | `spring.ai.model.chat=none`, `spring.ai.model.embedding=none`, `agent.execution.mode` 미지정 시 local | LLM 없이 결정론적 후보를 생성한다. 의미 검색은 embedding model이 없어 실패한다. | job 생성/worker/제안함 저장 E2E 가능. 실제 AI 품질 검증은 불가 |
| ai profile | `--spring.profiles.active=ai,...` 또는 profile include | Bedrock chat/embedding, pgvector, Redis queue 사용 | 실제 LLM 분석/생성/검색 E2E 가능 |
| noop | `agent.execution.mode=noop` 또는 dispatch noop | job 생성만 하고 실제 처리하지 않을 수 있음 | API 골격 확인용. 결과 E2E 불가 |

주요 설정:

- `agent.execution.mode`: `local`, `llm`, `noop`
- `agent.dispatch.adapter`: `redis`, `in-memory`, `noop`
- `agent.dispatch.worker.scheduler.enabled`: worker 자동 처리 여부
- `spring.ai.model.chat`: `none` 또는 `bedrock-converse`
- `spring.ai.model.embedding`: `none` 또는 `bedrock-titan`

## 2. 공통 응답/상태

### 2.1 AgentJobResponse

`/api/ai/...` 생성 API와 `/api/agent-jobs/{jobId}`에서 사용한다.

```json
{
  "jobId": "uuid",
  "jobType": "ANALYZE_RESOURCE",
  "status": "PENDING",
  "resourceId": "uuid",
  "roomId": "uuid",
  "errorCode": null,
  "errorMessage": null,
  "retryCount": 0,
  "suggestionIds": [],
  "resourceSummaryId": null,
  "aiDocumentId": null,
  "startedAt": null,
  "finishedAt": null
}
```

상태:

- `PENDING`
- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `CANCELED`

### 2.2 Suggestion 공통

제안함은 AI가 만든 후보를 확정 데이터로 바로 쓰지 않고, 사용자가 승인/수정/보류/거절할 수 있게 저장한다.

`suggestionType`:

- `REQUIREMENT`
- `TODO`
- `TASK`
- `WBS`
- `SCHEDULE`
- `QUESTION`
- `CONTRACT_FIELD`
- `CONTRACT_REVIEW`
- `REVIEW_ITEM`
- `DOCUMENT_DRAFT`
- `DAILY_SUMMARY`
- `MEMO`

리뷰 액션:

- `APPROVE`
- `EDIT`
- `MODIFY`
- `HOLD`
- `REJECT`
- `DELETE`

승인 시 실제 도메인 반영:

| suggestionType | 승인 결과 |
|---|---|
| `TASK`, `TODO` | 프로젝트룸 TODO 생성 |
| `WBS` | WBS item 생성 |
| `SCHEDULE` | 일정 생성 |
| `DAILY_SUMMARY` | daily summary draft upsert |
| `DOCUMENT_DRAFT` | `generated_documents` 생성. `appliedResult.targetType=GENERATED_DOCUMENT` |
| `REQUIREMENT` | 승인된 suggestion 자체를 확정 요구사항으로 보존. `appliedResult.targetType=CONFIRMED_REQUIREMENT` |
| `QUESTION` | 확인 필요 질문으로 보존. `appliedResult.targetType=CONFIRMATION_QUESTION` |
| `REVIEW_ITEM` | 확인 필요 검토 항목으로 보존. `appliedResult.targetType=CONFIRMATION_REVIEW_ITEM` |
| `CONTRACT_FIELD` | 프로젝트룸 참고 계약값으로 보존. 자동 반영하지 않음. `appliedResult.targetType=CONTRACT_FIELD_REFERENCE` |
| `CONTRACT_REVIEW` | 법률 판단 없이 계약 검토 참고 메모로 보존. `appliedResult.targetType=CONTRACT_REVIEW_NOTE` |
| `MEMO` | 승인된 suggestion 자체를 확정 메모로 보존. `appliedResult.targetType=CONFIRMED_MEMO` |

## 3. 기능별 명세

### 3.1 자료 분석 Job

자료 파일을 분석해 요약, AI 문서 분류, chunk embedding, 관련 자료 index를 만든다.

Endpoint:

`POST /api/ai/analyze-resource`

Request:

```json
{
  "resourceId": "uuid"
}
```

권한:

- 로그인 필요
- `resourceId`를 읽을 수 있어야 함

처리 결과:

- `resource_summaries` 저장
- `ai_documents` 저장
- `resource_embeddings` 저장
- 관련 자료 relation rebuild
- resource status `ANALYZED` 또는 `FAILED`

지원 문서 포맷:

- PDF
- TXT
- Markdown: `.md`, `.markdown`
- DOCX

제한:

- OCR, HWP는 미구현
- 빈 텍스트 추출 시 실패
- unsupported format은 `RESOURCE_415_001`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| API 단독 | 가능 | job 생성 가능 |
| local worker | 가능 | PDF/TXT/Markdown/DOCX 추출, 요약 preview, ai_document 저장 가능 |
| 실제 LLM 분석 | 가능 조건부 | `ai` profile, Bedrock chat 설정 필요 |
| 의미 검색까지 | 가능 조건부 | embedding model과 pgvector 필요 |

사용 순서:

1. 자료 업로드 또는 계약 문서 업로드로 `resourceId` 확보
2. `POST /api/ai/analyze-resource`
3. worker 처리 대기
4. `GET /api/agent-jobs/{jobId}`
5. `GET /api/resources/{resourceId}/summary`
6. `GET /api/resources/{resourceId}/ai-document`
7. `POST /api/ai/search-resource`

### 3.2 프로젝트룸 계약/요구사항 문서 업로드 + 자동 분석

계약서/요구사항 문서를 업로드하고, 선택적으로 분석 job을 자동 생성한다.

Endpoint:

`POST /api/project-rooms/{roomId}/contract-documents`

Content-Type:

`multipart/form-data`

Form fields:

- `documentType`: `CONTRACT` 또는 `REQUIREMENT`
- `file`: PDF/TXT/Markdown/DOCX

처리 결과:

- `resources`
- `resource_files`
- `resource_versions`
- autoAnalyze=true이면 `ANALYZE_RESOURCE` job 생성

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 업로드 E2E | 가능 | 멤버 권한, 중복 checksum, 파일 검증 포함 |
| 자동 분석 E2E | 가능 | worker가 활성화되어 있어야 함 |
| OCR/HWP | 불가 | 계획상 별도 확장 |

### 3.3 요구사항 후보 생성

프로젝트룸 context를 바탕으로 요구사항 후보를 만든다.

Endpoint:

`POST /api/ai/generate-requirements`

Request:

```json
{
  "roomId": "uuid"
}
```

결과:

- `agent_suggestions.suggestion_type = REQUIREMENT`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| API 단독 | 가능 | job 생성 |
| local worker | 가능 | deterministic 후보 1개 생성 |
| LLM 품질 E2E | 가능 조건부 | `agent.execution.mode=llm`, Bedrock chat 필요 |
| 승인 후 도메인 반영 | 가능 | 별도 requirements table 없이 승인된 suggestion 자체를 `CONFIRMED_REQUIREMENT` 확정 결과로 정의 |

사용 순서:

1. `POST /api/ai/generate-requirements`
2. `GET /api/agent-jobs/{jobId}`
3. `GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT&suggestionType=REQUIREMENT`
4. 필요 시 `PATCH /api/agent/suggestions/{suggestionId}` with `EDIT`, `HOLD`, `REJECT`, `APPROVE`

### 3.4 TODO/Task 후보 생성

프로젝트룸 context에서 작업 후보를 만든다.

Endpoint:

`POST /api/ai/generate-tasks`

Request:

```json
{
  "roomId": "uuid"
}
```

결과:

- local/LLM job 결과는 `TASK` 후보로 저장
- 채팅 `SUGGEST`는 경우에 따라 `TODO` 후보도 생성

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 | local/LLM 모두 가능 |
| 승인 후 TODO 생성 | 가능 | `TASK`/`TODO` 승인 시 `tasks` 생성 |
| 품질 있는 후보 | 가능 조건부 | LLM mode 필요 |

주의:

- 승인 시 payload에 `title`이 필수다.
- `description`, `assigneeUserId`, `wbsItemId`, `status`, `dueAt`은 선택이다.

### 3.5 WBS 후보 생성

Endpoint:

`POST /api/ai/generate-wbs`

Request:

```json
{
  "roomId": "uuid"
}
```

결과:

- `agent_suggestions.suggestion_type = WBS`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 |
| 승인 후 WBS 생성 | 가능 | payload `title` 필수 |
| LLM 기반 품질 | 가능 조건부 | Bedrock chat 필요 |

### 3.6 확인 질문 후보 생성

Endpoint:

`POST /api/ai/generate-questions`

Request:

```json
{
  "roomId": "uuid"
}
```

결과:

- `agent_suggestions.suggestion_type = QUESTION`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 |
| 승인 후 별도 질문 도메인 생성 | 가능 | 별도 질문 테이블 없이 승인된 suggestion 자체를 `CONFIRMATION_QUESTION` 확인 항목으로 조회 |
| 제안함 조회/수정/보류/거절 | 가능 |

### 3.7 계약 문서 검토 후보 생성

Endpoint:

`POST /api/ai/review-contract-documents`

Request:

```json
{
  "roomId": "uuid"
}
```

결과:

- `agent_suggestions.suggestion_type = REVIEW_ITEM`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 |
| 계약서/요구사항 비교 고품질 분석 | 가능 조건부 | LLM mode와 분석된 문서 context 필요 |
| 승인 후 계약 도메인 반영 | 가능 | 계약 필드는 `CONTRACT_FIELD_REFERENCE`, 계약 검토는 `CONTRACT_REVIEW_NOTE`로 보존. 자동 반영 및 법률 판단 없음 |

주의:

- 법적 확정 판단을 하지 않고 검토 보조 후보로 저장하는 구조다.

### 3.8 하루 정리 후보 생성

개인 단위 daily summary 후보를 생성한다.

Endpoint:

`POST /api/ai/summarize-day`

Request:

```json
{
  "summaryDate": "2026-07-01",
  "timezone": "Asia/Seoul"
}
```

`summaryDate` 생략 시 실행 시점 기준 날짜가 사용된다. `timezone` 생략 시 `Asia/Seoul` 기준으로 하루 범위를 계산한다.

결과:

- `agent_suggestions.suggestion_type = DAILY_SUMMARY`

승인 결과:

- `daily_summaries` draft upsert

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 |
| 승인 후 daily summary 조회 | 가능 | `GET /api/daily-summaries?from=...&to=...` |
| 고품질 context 기반 요약 | 부분 가능 | TODO/일정/채팅/메모리 context 중심. activity/widget/memo 고도화는 별도 gap |

### 3.9 문서 초안 생성

문서 초안 후보를 만든다.

Endpoint:

`POST /api/ai/draft-document`

Request:

```json
{
  "roomId": "uuid",
  "documentType": "MEETING_NOTE",
  "sourceResourceIds": ["uuid"],
  "instruction": "Draft a concise meeting note with decisions and action items."
}
```

결과:

- `agent_suggestions.suggestion_type = DOCUMENT_DRAFT`
- local mode에서는 payload에 `contentMarkdown`이 들어간 deterministic draft 생성

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 후보 생성 | 가능 |
| 제안함 조회/수정/승인 | 가능 |
| 승인 후 별도 문서 결과 조회 | 가능 | 승인 시 `generated_documents` row 생성 |

### 3.10 자료 의미 검색

분석/임베딩된 자료 chunk를 embedding similarity로 검색한다.

Endpoint:

`POST /api/ai/search-resource`

Request - 개인 자료:

```json
{
  "scope": "PERSONAL",
  "query": "meeting decisions",
  "topK": 5
}
```

Request - 프로젝트룸 자료:

```json
{
  "scope": "ROOM_SHARED",
  "roomId": "uuid",
  "query": "project requirements",
  "topK": 5
}
```

Response:

```json
{
  "hits": [
    {
      "embeddingId": "uuid",
      "resourceId": "uuid",
      "chunkIndex": 0,
      "chunkText": "...",
      "pageNumber": 1,
      "chunkMetadata": "{\"pageNumber\":1}",
      "similarityScore": 0.82
    }
  ]
}
```

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| API 단독 | 가능 |
| local 기본 설정 | 불가 | `EmbeddingModel is not available` |
| 실제 검색 | 가능 조건부 | `ai` profile, Bedrock embedding, pgvector, 분석된 embedding 필요 |

사용 전제:

1. 자료 업로드
2. `ANALYZE_RESOURCE` 성공
3. `resource_embeddings` 저장
4. embedding model 활성화

### 3.11 AI 문서 조회

자료 분석 후 AI가 읽을 수 있는 문서 분류 상태를 조회한다.

Endpoint:

`GET /api/resources/{resourceId}/ai-document`

Endpoint:

`GET /api/project-rooms/{roomId}/ai-documents?status=READY&page=0&size=20`

Response:

```json
{
  "id": "uuid",
  "resourceId": "uuid",
  "roomId": "uuid",
  "documentType": "CONTRACT",
  "detectedConfidence": 0.8,
  "status": "READY",
  "createdAt": "...",
  "updatedAt": "..."
}
```

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 단건 조회 | 가능 | 분석 성공 후 가능 |
| 룸 목록 조회 | 가능 | active member 권한 필요 |

주의:

- 현재 agent 도메인과 resource 도메인에 `AiDocument` 모델이 함께 존재한다. API는 agent controller를 통해 조회하지만 저장은 resource 분석 흐름에서 수행된다.

### 3.12 생성 문서 조회

`DOCUMENT_DRAFT` suggestion을 승인하면 `generated_documents`에 문서 결과가 저장된다.

Endpoint:

`GET /api/generated-documents?page=0&size=20`

개인 생성 문서 목록을 조회한다.

Endpoint:

`GET /api/project-rooms/{roomId}/generated-documents?page=0&size=20`

프로젝트룸 생성 문서 목록을 조회한다.

Endpoint:

`GET /api/generated-documents/{documentId}`

생성 문서 상세를 조회한다.

Response:

```json
{
  "id": "uuid",
  "userId": "uuid",
  "roomId": "uuid",
  "suggestionId": "uuid",
  "resourceId": "uuid",
  "title": "회의록 초안",
  "documentType": "MEETING_NOTE",
  "contentMarkdown": "# Meeting Note",
  "metadataJson": {
    "source": "AGENT_SUGGESTION",
    "reviewerId": "uuid",
    "sourceResourceIds": []
  },
  "createdAt": "...",
  "updatedAt": "..."
}
```

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 문서 초안 후보 생성 | 가능 | `/api/ai/draft-document` |
| 승인 후 생성 문서 저장 | 가능 | `DOCUMENT_DRAFT` 승인 시 생성 |
| 목록/상세 조회 | 가능 | room 문서는 active member 권한 필요 |
| export/download | 미구현 | 현재는 Markdown content 조회 |

### 3.13 Agent Job 상태/이벤트 조회

Endpoint:

`GET /api/agent-jobs/{jobId}`

Endpoint:

`GET /api/agent-jobs/{jobId}/events?page=0&size=20`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 상태 조회 | 가능 |
| 이벤트 조회 | 가능 | requester 또는 room active member 권한 필요 |
| WebSocket 이벤트 | 부분 가능 | project room event는 일부 도메인에서 발행. agent job event 자체 WebSocket은 별도 확인 필요 |

### 3.14 제안함 조회/수정/승인

Endpoint:

`GET /api/agent/suggestions?status=DRAFT&suggestionType=DAILY_SUMMARY`

Endpoint:

`GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT&suggestionType=TASK`

Endpoint:

`PATCH /api/agent/suggestions/{suggestionId}`

Edit request:

```json
{
  "action": "EDIT",
  "editedContent": {
    "title": "Edited suggestion title",
    "description": "Edited content"
  }
}
```

Approve request:

```json
{
  "action": "APPROVE"
}
```

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| 조회/수정/보류/거절/삭제 | 가능 |
| `TASK`/`TODO` 승인 반영 | 가능 |
| `WBS` 승인 반영 | 가능 |
| `SCHEDULE` 승인 반영 | 가능. 단 payload에 `startsAt` 필수 |
| `DAILY_SUMMARY` 승인 반영 | 가능 |
| 그 외 타입 승인 반영 | 가능. 타입별 확정 보존 targetType으로 처리 |

### 3.15 프로젝트룸 채팅 에이전트 명령

프로젝트룸 채팅 흐름에서 에이전트 응답과 room memory draft를 만든다.

Endpoint:

`POST /api/project-rooms/{roomId}/agent/commands`

Request:

```json
{
  "message": "계약 리스크 검토 항목을 제안해줘",
  "mode": "SUGGEST",
  "resourceIds": ["uuid"]
}
```

mode:

- `ANSWER`: 답변만 생성
- `SUMMARIZE`: 프로젝트 context 요약
- `SUGGEST`: 답변 + `AgentSuggestion` draft 생성

Response:

```json
{
  "message": {
    "id": "uuid",
    "roomSequence": 10,
    "messageType": "AGENT_RESPONSE",
    "body": {
      "text": "...",
      "request": "...",
      "mode": "SUGGEST",
      "promptVersion": "project-room-agent-command-v1",
      "contextCharacters": 123,
      "suggestionIds": ["uuid"]
    }
  },
  "memorySummary": {
    "id": "uuid",
    "fromSequence": 10,
    "toSequence": 10,
    "status": "DRAFT"
  },
  "suggestions": [
    {
      "suggestionId": "uuid",
      "suggestionType": "REVIEW_ITEM",
      "status": "DRAFT"
    }
  ]
}
```

SUGGEST 타입 추론:

- 질문/확인/unclear/missing 계열: `QUESTION`
- 검토/리스크/계약/review/risk/contract 계열: `REVIEW_ITEM`
- 그 외: `TODO`

E2E 가능 여부:

| 수준 | 가능 여부 | 설명 |
|---|---|---|
| ANSWER/SUMMARIZE | 가능 | ChatModel 없으면 fallback 응답 |
| SUGGEST 후보 생성 | 가능 | LLM 없어도 후보 생성 |
| 고품질 자연어 응답 | 가능 조건부 | ChatModel 필요 |
| 제안함 조회 연계 | 가능 | room suggestions에서 조회 가능 |

## 4. E2E 시나리오

### 4.1 로컬 deterministic E2E

목적: 외부 LLM 없이 job 생성, worker, 제안함, 승인 흐름 검증

전제 설정:

- `agent.execution.mode=local`
- queue/worker가 실제 처리되도록 `agent.dispatch.adapter=in-memory` 또는 Redis 구성
- `agent.dispatch.worker.scheduler.enabled=true`

순서:

1. 로그인해서 access token 확보
2. 프로젝트룸 생성 및 멤버 권한 확보
3. `POST /api/project-rooms/{roomId}/contract-documents`로 TXT/Markdown/DOCX 업로드
4. 반환된 `jobId`를 `GET /api/agent-jobs/{jobId}`로 확인
5. `GET /api/resources/{resourceId}/ai-document`
6. `POST /api/ai/generate-tasks`
7. `GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT`
8. `PATCH /api/agent/suggestions/{suggestionId}` with `APPROVE`
9. `GET /api/project-rooms/{roomId}/tasks`

가능:

- 업로드
- 분석
- 제안 생성
- TODO/WBS/DAILY_SUMMARY 일부 승인 반영

불가/제한:

- 실제 의미 검색은 embedding model 필요
- 생성 내용은 deterministic placeholder

### 4.2 실제 LLM/RAG E2E

목적: Bedrock chat/embedding과 pgvector 기반 실제 AI 흐름 검증

전제 설정:

- `spring.profiles.active=ai,...`
- `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` 설정
- 필요 시 `AWS_SESSION_TOKEN`
- `agent.execution.mode=llm`
- `agent.dispatch.adapter=redis`
- Redis 연결
- PostgreSQL pgvector migration 적용
- `spring.ai.model.chat=bedrock-converse`
- `spring.ai.model.embedding=bedrock-titan`

순서:

1. 로그인
2. 프로젝트룸 생성
3. 문서 업로드
4. `ANALYZE_RESOURCE` 성공 확인
5. `POST /api/ai/search-resource`
6. `POST /api/ai/generate-requirements`
7. `POST /api/ai/review-contract-documents`
8. 제안함 조회 및 승인/보류/거절
9. `POST /api/project-rooms/{roomId}/agent/commands` with `SUGGEST`

가능:

- 문서 분석 LLM JSON 계약 검증
- suggestion 생성
- embedding 검색
- 모델 호출 로그 저장

주의:

- `agent_model_call_logs` token/cost는 현재 실제 provider token이 아니라 추정 또는 제한적 기록일 수 있다.
- LLM output이 schema validation에 실패하면 job이 실패한다.

## 5. 현재 E2E 가능 여부 요약

| 기능 | API | local E2E | 실제 AI E2E | 비고 |
|---|---|---:|---:|---|
| 자료 분석 | `/api/ai/analyze-resource` | 가능 | 가능 조건부 | PDF/TXT/MD/DOCX |
| 계약 문서 업로드 자동 분석 | `/contract-documents` | 가능 | 가능 조건부 | OCR/HWP 제외 |
| 요구사항 후보 | `/api/ai/generate-requirements` | 가능 | 가능 조건부 | 승인은 보존 처리 |
| TODO 후보 | `/api/ai/generate-tasks` | 가능 | 가능 조건부 | 승인 후 task 생성 가능 |
| WBS 후보 | `/api/ai/generate-wbs` | 가능 | 가능 조건부 | 승인 후 WBS 생성 가능 |
| 확인 질문 후보 | `/api/ai/generate-questions` | 가능 | 가능 조건부 | 승인은 보존 처리 |
| 계약 검토 후보 | `/api/ai/review-contract-documents` | 가능 | 가능 조건부 | 승인은 보존 처리 |
| 하루 정리 후보 | `/api/ai/summarize-day` | 가능 | 가능 조건부 | 승인 후 daily summary draft |
| 문서 초안 후보 | `/api/ai/draft-document` | 가능 | 가능 조건부 | 승인 후 `generated_documents` 조회 가능 |
| 생성 문서 조회 | `/api/generated-documents`, `/api/project-rooms/{roomId}/generated-documents` | 가능 | 가능 | export/download는 미구현 |
| 의미 검색 | `/api/ai/search-resource` | 불가 | 가능 조건부 | embedding model 필수 |
| AI 문서 조회 | `/ai-document`, `/ai-documents` | 가능 | 가능 | 분석 결과 필요 |
| 제안함 조회/수정/승인 | `/api/agent/suggestions` | 가능 | 가능 | 타입별 반영 차이 |
| 채팅 에이전트 | `/agent/commands` | 가능 | 가능 조건부 | ChatModel 없으면 fallback |

## 6. 테스트/검증 포인트

이미 있는 HTTP 예제:

- `docs/http/agent.http`

추천 수동 검증 순서:

1. `POST /api/ai/analyze-resource`
2. `GET /api/agent-jobs/{jobId}`
3. `GET /api/agent-jobs/{jobId}/events`
4. `GET /api/resources/{resourceId}/ai-document`
5. `POST /api/ai/generate-tasks`
6. `GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT`
7. `PATCH /api/agent/suggestions/{suggestionId}` with `APPROVE`
8. `GET /api/project-rooms/{roomId}/tasks`
9. `POST /api/project-rooms/{roomId}/agent/commands` with `SUGGEST`

추천 자동 테스트 범위:

- `com.bubli.agent.service.*`
- `com.bubli.agent.dispatch.*`
- `com.bubli.resource.service.ResourceAnalysisPublicServiceTest`
- `com.bubli.resource.service.DocumentFileInspectorTest`
- `com.bubli.architecture.ArchitectureTest`

## 7. 미구현/부분 구현 목록

- OCR 분석
- HWP 분석
- 생성 문서 export/download
- 요구사항/질문/검토 항목의 별도 확정 도메인 반영
- 의미 검색 local fallback
- 실제 token/cost 정밀 기록
- Agent job 자체의 WebSocket 상태 이벤트 완전 연결 여부는 추가 확인 필요
