# RAG Step 5, Step 5.5, Step 6 Flow

이 문서는 merge 이후 현재 코드 기준으로 Step 5, Step 5.5, Step 6이 어떻게 동작하는지 설명한다.

현재 구현은 크게 세 흐름으로 나뉜다.

- Step 5: 자료 분석 결과를 `resource_embeddings`에 저장하고 semantic search에 사용한다.
- Step 5.5: page number, 권한 검증, PERSONAL/ROOM_SHARED 검색, vector literal 변환 중복 제거를 보강한다.
- Step 6: agent가 만든 후보를 `agent_suggestions`에 저장하고 사용자가 조회, 승인, 보류, 거절, 수정한다.

merge 이후 Agent job 생성과 dispatch 구조가 들어오면서, Step 5/6은 더 이상 단순한 synchronous 분석 흐름만으로 이해하면 안 된다. 현재는 `agent_jobs`를 만들고, dispatch queue/outbox/worker가 job을 실행하거나 실행 결과를 기록하는 구조가 함께 존재한다.

## 현재 완료 상태

| 영역 | 현재 상태 |
|---|---|
| 자료 분석 job 생성 | 구현됨 |
| 자료 분석 결과 요약 저장 | 구현됨 |
| AI 문서 분류 저장/조회 | 구현됨 |
| resource embedding 저장 | 구현됨 |
| ROOM_SHARED semantic search | 구현됨 |
| PERSONAL semantic search | 구현됨 |
| 검색 시 room membership 검증 | 구현됨 |
| PDF page number 추적 | 구현됨 |
| Agent suggestion 조회/검토 | 기본 구현됨 |
| Agent job 상태 조회 | 구현됨 |
| Agent job event 조회 API | 구현됨 |
| TODO/WBS 후보 생성 job | job 생성만 구현됨 |
| TODO/WBS 실제 AI 후보 생성 | execution port가 Noop이므로 미구현 |
| suggestion 승인 후 원본 도메인 반영 | 미구현 |
| room suggestion 조회 권한 검증 | 보강 필요 |
| suggestion 삭제 | 미구현 |

## 전체 흐름

### 자료 업로드 후 분석 흐름

```text
사용자
-> POST /api/project-rooms/{roomId}/contract-documents
-> DocumentUploadService
-> Resource / ResourceFile / ResourceVersion 저장
-> AgentJobPublicService.createAnalyzeResourceJob(...)
-> agent_jobs에 ANALYZE_RESOURCE job 저장
-> 분석 job은 이후 worker 또는 service가 처리
```

### 수동 분석 job 생성 흐름

```text
사용자
-> POST /api/ai/analyze-resource
-> AiJobCommandController
-> AiJobCommandService.createAnalyzeResourceJob(...)
-> ResourcePublicService.getReadableResource(...)로 접근 권한 확인
-> AgentJobService.create(...)
-> agent_jobs 저장
-> AgentJobDispatchOutboxRecorder.recordPending(...)
-> AgentJobDispatchEvent publish
-> AgentJobDispatchEventListener가 dispatch 시도
```

현재 `/api/ai/analyze-resource`는 `AiJobCommandController` 한 곳에서만 담당한다. merge 직후에는 `AgentJobController`에도 같은 endpoint가 있어 ambiguous mapping 오류가 있었고, 중복 endpoint는 제거했다.

### dispatch 실행 흐름

```text
agent_jobs PENDING
-> dispatch queue 또는 outbox
-> AgentJobDispatchWorker.processNextQueuedJob()
-> job 상태 RUNNING
-> AgentJobExecutionPort.execute(...)
-> 성공 시 suggestion/model call log 저장
-> AgentJobExecutionResultRecorder.recordSucceeded(...)
-> job 상태 SUCCEEDED

실패 시:
-> AgentJobExecutionResultRecorder.recordFailed(...)
-> job 상태 FAILED
-> retryCount는 증가하지 않음

dispatch/enqueue 실패 시:
-> AgentJob.markDispatchFailed(...)
-> job 상태 FAILED
-> retryCount 증가
```

`retryCount`는 현재 실행 실패 횟수가 아니라 dispatch/retry 계열 실패 횟수로 취급한다. 이 의미를 맞추기 위해 `AgentJob.fail(...)`에서는 retry count를 증가시키지 않고, `markDispatchFailed(...)`에서만 증가시킨다.

## Step 5: Resource Embedding Indexing

### 핵심 테이블

`resource_embeddings`

| 컬럼 | 의미 |
|---|---|
| `resource_id` | 원본 자료 ID |
| `owner_id` | PERSONAL 검색 권한 필터 |
| `room_id` | ROOM_SHARED 검색 권한 필터 |
| `visibility` | `PERSONAL` 또는 `ROOM_SHARED` |
| `chunk_index` | 자료 내 chunk 순서 |
| `chunk_text` | 검색 결과로 반환할 원문 일부 |
| `embedding` | pgvector `vector(1024)` |
| `chunk_metadata` | 파일명, MIME type, pageNumber, offset 등 |

### 분석 진입점

파일:

- `src/main/java/com/bubli/resource/service/ResourceAnalysisPublicService.java`

주요 메서드:

```java
public void analyzeResourceForJob(UUID resourceId, UUID jobId)
```

역할:

- resource 도메인이 분석 대상 자료를 조회한다.
- PDF/TXT에서 텍스트를 추출한다.
- `resource_summaries`를 저장한다.
- `ai_documents`를 저장하거나 기존 row를 사용한다.
- `resource_embeddings` indexing을 호출한다.
- 성공 시 resource 상태를 `ANALYZED`로 변경한다.
- 실패 시 resource 상태를 `FAILED`로 변경한다.

처리 순서:

```text
Resource 조회
-> ResourceFile 최신 버전 조회
-> PDF/TXT 텍스트 추출
-> ResourceSummary 저장
-> AiDocument 저장
-> ResourceEmbeddingIndexPublicService.index(...)
-> Resource.markAnalyzed()
```

### PDF page number 추적

파일:

- `src/main/java/com/bubli/resource/service/ResourceAnalysisPublicService.java`
- `src/main/java/com/bubli/resource/service/TextChunker.java`

PDF는 전체 텍스트를 한 번에 뽑지 않고 page 단위로 추출한다.

```java
stripper.setStartPage(pageNumber);
stripper.setEndPage(pageNumber);
pages.add(new TextChunker.TextPage(pageNumber, stripper.getText(document)));
```

TXT는 page 개념이 없으므로 `pageNumber = null`로 처리한다.

`TextChunker`는 page 단위 입력을 받아 chunk를 만든다.

```java
public List<TextChunk> splitPages(List<TextPage> pages)
```

`TextChunk`에는 다음 정보가 포함된다.

```java
public record TextChunk(
        int index,
        String text,
        int startOffset,
        int endOffset,
        Integer pageNumber
) {}
```

결과적으로 PDF 검색 결과는 chunk가 어느 page에서 왔는지 추적할 수 있다.

### Embedding 저장

파일:

- `src/main/java/com/bubli/resource/service/ResourceEmbeddingIndexPublicService.java`
- `src/main/java/com/bubli/resource/entity/ResourceEmbedding.java`
- `src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java`

주요 메서드:

```java
public IndexResult index(Resource resource, ResourceFile resourceFile, List<TextChunker.TextPage> pages)
```

처리:

```text
EmbeddingModel 조회
-> TextChunker.splitPages(...)
-> 기존 resource embedding 삭제
-> chunk마다 embedding 생성
-> EmbeddingVectorFormatter.toVectorLiteral(...)
-> ResourceEmbedding.create(...)
-> saveAll(...)
```

저장 metadata 예시:

```json
{
  "originalName": "requirements.pdf",
  "mimeType": "application/pdf",
  "pageNumber": 3,
  "startOffset": 0,
  "endOffset": 450,
  "characterCount": 450
}
```

### Vector literal 변환

파일:

- `src/main/java/com/bubli/resource/service/EmbeddingVectorFormatter.java`

역할:

- embedding vector를 pgvector literal 문자열로 변환한다.
- indexing/search 양쪽에서 같은 검증 로직을 사용한다.

검증:

- vector가 `null`이면 실패
- dimension이 1024가 아니면 실패
- `NaN`, `Infinity`가 있으면 실패

출력 형식:

```text
[0.1,0.2,0.3]
```

native query에서는 다음처럼 사용한다.

```sql
CAST(:queryEmbedding AS vector)
```

## Step 5.5: Semantic Search 개선

### 검색 API

파일:

- `src/main/java/com/bubli/agent/controller/AgentJobController.java`
- `src/main/java/com/bubli/agent/dto/SearchResourceRequest.java`
- `src/main/java/com/bubli/agent/dto/SearchResourceResponse.java`
- `src/main/java/com/bubli/resource/service/ResourceSemanticSearchPublicService.java`

Endpoint:

```http
POST /api/ai/search-resource
Authorization: Bearer {accessToken}
Content-Type: application/json
```

ROOM_SHARED 검색:

```json
{
  "scope": "ROOM_SHARED",
  "roomId": "00000000-0000-0000-0000-000000000000",
  "query": "로그인 요구사항",
  "topK": 5
}
```

PERSONAL 검색:

```json
{
  "scope": "PERSONAL",
  "query": "개인 메모",
  "topK": 5
}
```

### 검색 서비스

파일:

- `src/main/java/com/bubli/resource/service/ResourceSemanticSearchPublicService.java`

주요 메서드:

```java
public List<ResourceSearchHit> search(
        UUID userId,
        ResourceSearchScope scope,
        UUID roomId,
        String query,
        Integer topK
)
```

처리 순서:

```text
userId 검증
-> query blank 검증
-> EmbeddingModel 조회
-> query embedding 생성
-> vector literal 변환
-> topK 보정
-> scope별 검색
```

scope별 동작:

- `PERSONAL`: `owner_id = currentUser.userId()`인 embedding만 검색한다.
- `ROOM_SHARED`: `roomId`가 필수이고, 사용자가 room member인지 검증한 뒤 검색한다.

### Room membership 검증

파일:

- `src/main/java/com/bubli/project/service/ProjectRoomAccessPublicService.java`
- `src/main/java/com/bubli/project/repository/RoomMemberRepository.java`
- `src/main/java/com/bubli/resource/service/ResourceSemanticSearchPublicService.java`

검색 시 resource 도메인은 project repository를 직접 참조하지 않는다. 대신 public service 경계를 사용한다.

```java
projectRoomAccessService.requireRoomMember(roomId, userId);
```

이 구조는 architecture rule을 지키기 위한 것이다.

### 검색 repository

파일:

- `src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java`

ROOM_SHARED:

```java
List<ResourceEmbeddingSearchRow> searchRoomShared(
        UUID roomId,
        String queryEmbedding,
        int limit
)
```

조건:

```sql
WHERE room_id = :roomId
  AND visibility = 'ROOM_SHARED'
```

PERSONAL:

```java
List<ResourceEmbeddingSearchRow> searchPersonal(
        UUID ownerId,
        String queryEmbedding,
        int limit
)
```

조건:

```sql
WHERE owner_id = :ownerId
  AND visibility = 'PERSONAL'
```

정렬:

```sql
ORDER BY embedding <=> CAST(:queryEmbedding AS vector)
```

### 검색 응답

파일:

- `src/main/java/com/bubli/resource/dto/ResourceSearchHit.java`

응답 필드:

```java
public record ResourceSearchHit(
        UUID embeddingId,
        UUID resourceId,
        int chunkIndex,
        String chunkText,
        Integer pageNumber,
        String chunkMetadata,
        double similarityScore
) {}
```

`pageNumber`는 `chunk_metadata` JSON에서 별도 필드로 추출해 응답한다.

## Step 6: Agent Suggestion

Step 6은 AI가 만든 후보를 저장하고, 사용자가 검토 상태를 변경하는 기능이다.

현재 구현은 suggestion 저장/조회/검토 상태 변경까지다. 승인된 suggestion을 실제 TODO, WBS, Schedule 등 원본 도메인 객체로 확정 반영하는 기능은 아직 없다.

### 핵심 테이블

`agent_suggestions`

| 컬럼 | 의미 |
|---|---|
| `user_id` | 후보 소유 사용자 |
| `room_id` | 프로젝트룸 후보인 경우 room |
| `job_id` | 후보를 만든 agent job |
| `resource_id` | 근거 자료 |
| `suggestion_type` | `TASK`, `REQUIREMENT`, `REVIEW_ITEM`, `QUESTION` 등 |
| `payload_json` | 후보 내용 |
| `evidence_json` | 근거 정보 |
| `status` | `DRAFT`, `APPROVED`, `HELD`, `REJECTED` |
| `reviewed_by` | 검토자 |
| `reviewed_at` | 검토 시각 |

`reviewed_by`, `reviewed_at`은 `V8__agent_suggestions_review_metadata.sql`에서 추가한다.

### Entity

파일:

- `src/main/java/com/bubli/agent/entity/AgentSuggestion.java`

생성:

```java
AgentSuggestion.draft(...)
AgentSuggestion.createDraft(...)
```

상태 변경:

```java
approve(reviewerId)
hold(reviewerId)
reject(reviewerId)
modify(reviewerId, modifiedPayloadJson)
```

규칙:

- `DRAFT` 상태에서만 검토 상태 변경 가능
- `APPROVED`, `HELD`, `REJECTED`가 된 suggestion은 다시 변경할 수 없음
- `MODIFY`는 payload를 교체하고 `reviewedBy`, `reviewedAt`을 기록함

### Suggestion 조회 API

파일:

- `src/main/java/com/bubli/agent/controller/AgentSuggestionController.java`
- `src/main/java/com/bubli/agent/service/AgentSuggestionQueryService.java`

개인 suggestion 조회:

```http
GET /api/agent/suggestions?status=DRAFT&suggestionType=TASK
Authorization: Bearer {accessToken}
```

프로젝트룸 suggestion 조회:

```http
GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT&suggestionType=TASK
Authorization: Bearer {accessToken}
```

주의:

- 개인 suggestion 조회는 current user 기준이다.
- 프로젝트룸 suggestion 조회는 현재 코드에서 membership 검증 보강이 필요하다.

### Suggestion 검토 API

파일:

- `src/main/java/com/bubli/agent/controller/AgentSuggestionController.java`
- `src/main/java/com/bubli/agent/service/AgentSuggestionCommandService.java`

Endpoint:

```http
PATCH /api/agent/suggestions/{suggestionId}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

승인:

```json
{
  "action": "APPROVE"
}
```

보류:

```json
{
  "action": "HOLD"
}
```

거절:

```json
{
  "action": "REJECT"
}
```

수정:

```json
{
  "action": "MODIFY",
  "payloadJson": {
    "title": "로그인 API 구현",
    "description": "JWT 기반 로그인 API를 구현한다.",
    "priority": "HIGH"
  }
}
```

### Agent execution 결과와 suggestion 저장

파일:

- `src/main/java/com/bubli/agent/dispatch/AgentJobDispatchWorker.java`
- `src/main/java/com/bubli/agent/dispatch/AgentJobExecutionPort.java`
- `src/main/java/com/bubli/agent/dispatch/AgentJobExecutionOutcome.java`
- `src/main/java/com/bubli/agent/dispatch/AgentJobExecutionSuggestionRecorder.java`
- `src/main/java/com/bubli/agent/service/AgentSuggestionService.java`

현재 구조:

```text
AgentJobDispatchWorker
-> AgentJobExecutionPort.execute(message)
-> AgentJobExecutionOutcome.suggestionDrafts()
-> AgentJobExecutionSuggestionRecorder.recordSuggestions(...)
-> AgentSuggestionService.createDraft(...)
```

현재 기본 실행 port:

```java
NoopAgentJobExecutionPort
```

`NoopAgentJobExecutionPort`는 아무 결과도 반환하지 않는다.

```java
public Optional<AgentJobExecutionOutcome> execute(AgentJobQueueMessage message) {
    return Optional.empty();
}
```

따라서 TODO/WBS 후보 생성 job은 생성/dispatch 구조는 있으나, 실제 AI 후보를 생성하는 execution 구현은 아직 없다.

## Agent Job API와 상태 조회

### Job 생성 API

파일:

- `src/main/java/com/bubli/agent/controller/AiJobCommandController.java`
- `src/main/java/com/bubli/agent/service/AiJobCommandService.java`
- `src/main/java/com/bubli/agent/service/AgentJobService.java`

Endpoints:

```http
POST /api/ai/analyze-resource
POST /api/ai/generate-requirements
POST /api/ai/generate-tasks
POST /api/ai/generate-wbs
POST /api/ai/generate-questions
POST /api/ai/review-contract-documents
```

모든 room 기반 job은 `ProjectMembershipPublicService.assertActiveMember(...)`로 room membership을 확인한다.

자료 분석 job은 `ResourcePublicService.getReadableResource(...)`로 자료 접근 권한을 확인한다.

### Job 상태 조회 API

파일:

- `src/main/java/com/bubli/agent/controller/AgentJobController.java`
- `src/main/java/com/bubli/agent/service/AgentJobQueryService.java`

Endpoint:

```http
GET /api/agent-jobs/{jobId}
```

응답에는 다음 정보가 포함된다.

- job id
- job type
- status
- room id
- resource id
- error code/message
- retry count
- suggestion ids
- resource summary id
- ai document id
- started/finished time

### Job event 조회

파일:

- `src/main/java/com/bubli/agent/service/AgentJobService.java`
- `src/main/java/com/bubli/agent/dto/AgentJobEventResult.java`
- `src/main/java/com/bubli/agent/dto/AgentJobEventResponse.java`

service 메서드:

```java
public PageResponse<AgentJobEventResult> getRequestedJobEvents(
        UUID requestedByUserId,
        UUID jobId,
        Pageable pageable
)
```

현재 상태:

- service 레벨 구현은 있음
- controller endpoint `GET /api/agent-jobs/{jobId}/events` 구현됨
- 요청자 본인 또는 같은 room member만 event를 조회할 수 있음

## AI Document 조회

파일:

- `src/main/java/com/bubli/agent/controller/AiDocumentController.java`
- `src/main/java/com/bubli/agent/service/AiDocumentService.java`
- `src/main/java/com/bubli/agent/entity/AiDocument.java`
- `src/main/java/com/bubli/agent/repository/AiDocumentRepository.java`

Endpoints:

```http
GET /api/project-rooms/{roomId}/ai-documents
GET /api/resources/{resourceId}/ai-document
```

주의:

- merge 이후 `agent.entity.AiDocument`와 `resource.entity.AiDocument`가 동시에 존재한다.
- JPA entity name은 각각 `AgentAiDocument`, `ResourceAiDocument`로 분리했다.
- repository bean name도 `agentAiDocumentRepository`, `resourceAiDocumentRepository`로 분리했다.

## Flyway 변경 사항

현재 Step 5/6 관련 주요 migration:

| Migration | 역할 |
|---|---|
| `V1__init_schema.sql` | baseline schema |
| `V4__chat_message_client_id_scope.sql` | chat message unique scope 보정 |
| `V5__core_domain_fks_and_lookup_indexes.sql` | 주요 FK/index 추가 |
| `V6__agent_resource_spec_alignment.sql` | Resource/AgentJob 스키마 정렬 |
| `V7__resource_embeddings_and_legacy_rag_cleanup.sql` | resource_embeddings 보강, legacy RAG 테이블 제거 |
| `V8__agent_suggestions_review_metadata.sql` | `reviewed_by`, `reviewed_at` 추가 |
| `V9__agent_jobs_row_version.sql` | `agent_jobs.row_version` 추가 |

원칙:

- 이미 공유되었거나 적용된 migration은 수정하지 않는다.
- `V1`은 baseline으로 유지한다.
- 누락 컬럼은 `V8`, `V9`처럼 후속 migration으로 추가한다.
- `EntityFlywayAlignmentTest`는 `ADD COLUMN IF NOT EXISTS`를 올바르게 파싱하도록 수정되어 있다.

## Postman 검증 흐름

### Step 5 검색 검증

1. 프로젝트룸과 room member를 준비한다.
2. 문서를 업로드한다.

```http
POST /api/project-rooms/{roomId}/contract-documents
```

3. 분석 job이 생성되었는지 확인한다.
4. 현재 완전한 Postman E2E를 하려면 worker 실행 또는 별도 dev endpoint가 필요하다.
5. embedding이 저장된 뒤 검색한다.

```http
POST /api/ai/search-resource
Content-Type: application/json

{
  "scope": "ROOM_SHARED",
  "roomId": "{roomId}",
  "query": "로그인 요구사항",
  "topK": 5
}
```

PERSONAL:

```http
POST /api/ai/search-resource
Content-Type: application/json

{
  "scope": "PERSONAL",
  "query": "개인 자료 검색",
  "topK": 5
}
```

확인할 것:

- ROOM_SHARED 검색에서 room member가 아니면 403
- PERSONAL 검색은 current user의 자료만 반환
- PDF 검색 결과에 `pageNumber` 포함
- `chunkMetadata`에도 page 관련 정보 포함

### Step 6 suggestion 검증

현재는 suggestion seed 후 Postman으로 바로 검증하는 방식이 가장 단순하다.

seed 예시:

```sql
INSERT INTO agent_suggestions (
    id,
    user_id,
    room_id,
    job_id,
    resource_id,
    suggestion_type,
    payload_json,
    evidence_json,
    status,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    '{userId}'::uuid,
    '{roomId}'::uuid,
    NULL,
    NULL,
    'TASK',
    '{"title":"로그인 API 구현","description":"JWT 기반 로그인 API 구현"}'::jsonb,
    '{"sourceText":"로그인 기능 필요"}'::jsonb,
    'DRAFT',
    now(),
    now()
);
```

조회:

```http
GET /api/agent/suggestions?status=DRAFT
```

승인:

```http
PATCH /api/agent/suggestions/{suggestionId}
Content-Type: application/json

{
  "action": "APPROVE"
}
```

수정:

```http
PATCH /api/agent/suggestions/{suggestionId}
Content-Type: application/json

{
  "action": "MODIFY",
  "payloadJson": {
    "title": "로그인 API 구현",
    "description": "JWT 기반 로그인 API 구현",
    "priority": "HIGH"
  }
}
```

## 관련 테스트

주요 테스트:

- `EmbeddingVectorFormatterTest`
- `TextChunkerTest`
- `ResourceEmbeddingIndexPublicServiceTest`
- `ResourceSemanticSearchPublicServiceTest`
- `AgentSuggestionCommandServiceTest`
- `AgentSuggestionQueryServiceTest`
- `AgentJobExecutionResultRecorderTest`
- `AgentJobDispatchWorkerTest`
- `EntityFlywayAlignmentTest`
- `EntityMappingTest`
- `ArchitectureTest`
- `DomainDependencyArchitectureTest`

최근 확인한 명령:

```powershell
.\gradlew.bat test --tests "com.bubli.agent.dispatch.*" --console=plain
.\gradlew.bat test --tests com.bubli.schema.EntityFlywayAlignmentTest --tests com.bubli.EntityMappingTest --console=plain
.\gradlew.bat test --tests com.bubli.architecture.ArchitectureTest --tests com.bubli.architecture.DomainDependencyArchitectureTest --console=plain
```

## 남은 Step 구체화

이 목록은 현재 코드, `09_Data-Model.md`, `10_API-Design_revised.md`를 함께 기준으로 정리한 남은 작업이다. 단순히 controller method를 추가하는 수준이 아니라, API 계약, 권한, DB 반영, 비동기 실행, 테스트까지 완료되어야 해당 step이 끝난 것으로 본다.

### Step 6.1: Agent Job Event 조회 API

상태: 구현 완료

목표:

- `GET /api/agent-jobs/{jobId}/events`를 구현한다.
- `agent_job_events`에 기록된 이벤트를 시간순으로 조회할 수 있게 한다.
- Postman에서 job 생성 이후 dispatch/worker 진행 상태를 추적할 수 있게 한다.

현재 상태:

- `GET /api/agent-jobs/{jobId}`는 구현되어 있다.
- `GET /api/agent-jobs/{jobId}/events`도 구현되어 있다.
- 요청자 본인 또는 같은 room member만 event를 조회할 수 있다.

구현된 범위:

1. 기존 `AgentJobEventResponse`, `AgentJobEventResult` 사용
2. `AgentJobService.getAccessibleJobEvents(...)` 추가
3. `AgentJobController`에 `GET /api/agent-jobs/{jobId}/events` 추가
4. job 요청자 또는 room member만 조회 가능하도록 권한 검증
5. service test 추가

완료 기준:

- 존재하는 job의 이벤트 목록이 `createdAt` 기준으로 조회된다.
- 다른 사용자의 private job 또는 가입하지 않은 room job event는 조회할 수 없다.

### Step 6.2: Suggestion Review API 계약 정렬

상태: 구현 완료

목표:

- 설계서의 `PATCH /api/agent/suggestions/{id}` 계약과 현재 코드를 맞춘다.
- 현재 `MODIFY`, `payloadJson` 중심의 요청을 설계서 기준 `EDIT`, `editedContent`, `DELETE`까지 지원하는 형태로 보정한다.

현재 상태:

- action enum은 `APPROVE`, `EDIT`, `HOLD`, `REJECT`, `DELETE`, `MODIFY`를 지원한다.
- 신규 계약은 `EDIT`, `editedContent` 기준이다.
- 기존 호환을 위해 `MODIFY`, `payloadJson`도 임시 지원한다.
- `DELETE`는 repository delete로 처리한다.

구현된 범위:

1. `AgentSuggestionReviewAction`에 `EDIT`, `DELETE` 추가
2. 기존 `MODIFY`는 하위 호환 alias로 유지
3. request DTO에 `editedContent`를 추가하고 기존 `payloadJson`과의 호환 정책 정의
4. `EDIT` 처리 시 suggestion status는 `DRAFT`를 유지하고 payload만 교체
5. `DELETE` 처리 시 물리 삭제
6. room suggestion 검토 시 membership 검증

권장 정책:

- 내부 코드는 설계서 명칭인 `EDIT`를 기준으로 맞춘다.
- 이미 작성된 테스트나 클라이언트 호환이 필요하면 `MODIFY`는 임시 alias로만 유지한다.
- `DELETE`는 설계서가 physical delete를 전제로 하므로, 삭제 이력이 꼭 필요하지 않다면 repository delete로 구현한다.

완료 기준:

- `APPROVE`, `EDIT`, `HOLD`, `REJECT`, `DELETE` 요청이 모두 정상 동작한다.
- 잘못된 action 또는 필요한 payload가 없는 `EDIT` 요청은 400으로 실패한다.

### Step 6.3: Room Suggestion Membership 검증

상태: 구현 완료

목표:

- `GET /api/project-rooms/{roomId}/agent/suggestions`가 실제 room member만 접근 가능하도록 보강한다.
- 이미 구현된 room 기반 agent job 생성 API와 동일한 권한 정책을 suggestion 조회에도 적용한다.

현재 상태:

- `AgentSuggestionController.findRoomSuggestions`는 `@CurrentUser AuthUser`를 받는다.
- `AgentSuggestionQueryService.findRoomSuggestions(...)`는 조회 전에 `ProjectMembershipPublicService.assertActiveMember(...)`를 호출한다.
- `generate-requirements`, `generate-tasks`, `generate-wbs`, `generate-questions`, `review-contract-documents` job 생성은 이미 `ProjectMembershipPublicService.assertActiveMember(...)`로 room membership을 확인한다.

구현된 범위:

1. controller method에 `@CurrentUser AuthUser currentUser` 추가
2. query service에서 `room_members` 기반 membership 검증
3. 권한 실패 시 403 또는 프로젝트의 표준 예외로 응답
4. integration test 추가

완료 기준:

- room member는 project room suggestion을 조회할 수 있다.
- room member가 아닌 사용자는 같은 API에서 거부된다.

### Step 6.4: Agent Job 실제 실행 로직

상태: 1차 구현 완료

목표:

- job 생성만 하는 상태를 넘어, worker가 실제 결과를 만들고 `agent_suggestions`, `resource_summaries`, `resource_embeddings`, `ai_documents` 등에 반영하도록 한다.

현재 상태:

- `AgentJobType`에는 다음 타입이 존재한다.
  - `ANALYZE_RESOURCE`
  - `GENERATE_WBS`
  - `GENERATE_TASKS`
  - `GENERATE_REQUIREMENTS`
  - `REVIEW_CONTRACT_DOCUMENTS`
  - `GENERATE_QUESTIONS`
  - `DAILY_SUMMARY`
  - `DRAFT_DOCUMENT`
- 일부 API는 job 생성까지 구현되어 있다.
- 기본 execution port는 `LocalAgentJobExecutionPort`다.
- `agent.execution.mode=noop`일 때만 `NoopAgentJobExecutionPort`를 사용한다.
- 현재 실행 포트는 외부 LLM 호출이 아니라 local deterministic 결과를 생성한다.

구현된 범위:

1. `ANALYZE_RESOURCE`
   - resource text 추출
   - summary 저장
   - chunk 생성
   - embedding 저장
   - `ResourceAnalysisPublicService.analyzeResourceForJob(...)` 호출
2. `GENERATE_REQUIREMENTS`
   - 요구사항 후보 생성
   - `agent_suggestions.suggestion_type = REQUIREMENT`
3. `GENERATE_TASKS`
   - TASK 후보 생성
   - 설계서 기준 `TASK` 타입으로 정렬
4. `GENERATE_WBS`
   - WBS 후보 생성
   - `agent_suggestions.suggestion_type = WBS`
5. `GENERATE_QUESTIONS`
   - 질문 후보 생성
   - `agent_suggestions.suggestion_type = QUESTION`
6. `REVIEW_CONTRACT_DOCUMENTS`
   - 계약/문서 검토 항목 후보 생성
   - `agent_suggestions.suggestion_type = REVIEW_ITEM`
7. `DRAFT_DOCUMENT`
   - 문서 초안 생성
   - `agent_suggestions.suggestion_type = DOCUMENT_DRAFT`
8. `DAILY_SUMMARY`
   - 일일 요약 생성
   - `agent_suggestions.suggestion_type = DAILY_SUMMARY`

완료 기준:

- 각 job type이 worker에서 실행 가능하다.
- 성공 시 `agent_jobs.status = SUCCEEDED`가 된다.
- 실패 시 `FAILED` 또는 retry 상태와 event가 기록된다.
- 생성된 suggestion id는 job response에서 추적 가능하다.
- 외부 LLM 기반 고품질 후보 생성, `ai_documents`/`daily_summaries` 직접 저장, relation 자동 생성은 후속 step으로 남아 있다.

### Step 6.5: 승인된 Suggestion의 원본 도메인 반영

상태: 1차 구현 완료

목표:

- `agent_suggestions`는 최종 데이터가 아니라 후보 저장소다.
- 사용자가 승인한 후보를 실제 업무 도메인 테이블에 반영한다.

현재 상태:

- suggestion `APPROVE` 시 `AgentSuggestionDomainApplyService`가 실행된다.
- agent는 work repository/entity에 직접 접근하지 않고 `TaskPublicService`, `WbsItemPublicService`, `SchedulePublicService`를 통해 반영한다.

구현된 범위:

1. `TASK` 승인
   - 실제 task 또는 TODO 도메인 객체 생성
   - payload의 `title`, `description`, `assigneeUserId`, `wbsItemId`, `status`, `dueAt` 매핑
2. `WBS` 승인
   - `wbs_items` 생성
   - payload의 `title`, `parentId`, `orderNo`, `status` 매핑
3. `SCHEDULE` 승인
   - 일정 도메인 객체 생성
   - payload의 `title`, `startsAt`, `endsAt`, `allDay`, `taskId`, `wbsItemId` 매핑
4. 승인 중복 방지
   - 이미 `DRAFT`가 아닌 suggestion은 기존 상태 전이 규칙에 의해 재승인할 수 없다.
5. transaction 정합성
   - 도메인 반영 실패 시 suggestion 승인도 rollback된다.

완료 기준:

- 승인 API 호출 후 실제 도메인 데이터가 생성된다.
- 같은 suggestion을 중복 승인해도 중복 데이터가 생기지 않는다.
- 승인 실패 시 suggestion 상태와 원본 도메인 데이터가 불일치하지 않는다.
- `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY` 승인 후 전용 원본 도메인 반영은 후속 정책으로 남아 있다.

### Step 6.6: 누락 API 추가

상태: 구현 완료

목표:

- `10_API-Design_revised.md` 기준으로 Agent API surface를 맞춘다.

현재 구현된 API:

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
- `GET /api/daily-summaries`
- `PATCH /api/daily-summaries/{id}`
- `GET /api/agent/suggestions`
- `GET /api/project-rooms/{roomId}/agent/suggestions`
- `PATCH /api/agent/suggestions/{suggestionId}`
- `GET /api/project-rooms/{roomId}/ai-documents`
- `GET /api/resources/{resourceId}/ai-document`
- `GET /api/resources/{resourceId}/related`

추가된 API:

1. `POST /api/ai/draft-document`
2. `POST /api/ai/summarize-day`
3. `GET /api/daily-summaries`
4. `PATCH /api/daily-summaries/{id}`

완료 기준:

- 설계서에 있는 Agent/RAG 관련 API가 controller에 존재한다.
- DTO 필드명이 설계서와 맞는다.
- OpenAPI/Postman 예시와 실제 응답이 일치한다.

### Step 6.7: Resource Relation 자동 생성

목표:

- `resource_relations`를 단순 조회가 아니라 분석 결과로 자동 생성할 수 있게 한다.

현재 상태:

- `GET /api/resources/{resourceId}/related` 조회 API는 존재한다.
- resource 분석 중 embedding indexing이 성공하면 `ResourceRelationIndexPublicService`가 relation을 자동 재생성한다.
- relation은 같은 room 안의 resource embedding similarity를 기준으로 양방향 생성된다.

구현 범위:

1. 분석 완료 시 유사 embedding 기반 related resource 후보 계산
2. 같은 room 또는 접근 가능한 scope 안에서만 relation 생성
3. relation reason, score 정책 정의
4. 중복 relation 방지
5. relation 조회 테스트 추가

완료 기준:

- resource 분석 이후 관련 문서 조회 API에서 실제 관계 데이터가 반환된다.
- 권한이 없는 resource는 relation 후보에 포함되지 않는다.

상태: 구현 완료

### Step 6.8: Postman E2E 검증 시나리오

목표:

- Step 5, 5.5, 6을 개발자가 Postman으로 직접 검증할 수 있게 한다.

필수 시나리오:

1. 로그인 후 access token 확보
2. project room 생성 또는 기존 room 선택
3. resource 업로드
4. `POST /api/ai/analyze-resource`
5. `GET /api/agent-jobs/{jobId}`
6. `GET /api/agent-jobs/{jobId}/events`
7. `POST /api/ai/search-resource`
8. `POST /api/ai/generate-tasks`
9. suggestion 조회
10. suggestion `EDIT`, `APPROVE`, `HOLD`, `REJECT`, `DELETE`
11. 승인된 suggestion이 원본 도메인에 반영됐는지 조회

완료 기준:

- local/dev 환경에서 위 흐름을 순서대로 수행할 수 있다.
- async worker 실행 방식이 문서화되어 있다.
- 실패 케이스도 Postman으로 확인할 수 있다.

상태: 문서화 완료

검증 문서:

- `docs/RAG_POSTMAN_E2E.md`
- `docs/http/agent.http`

### Step 6.9: 테스트와 Schema 정합성 마무리

목표:

- Flyway, entity, controller, architecture test가 전체 기준으로 통과하도록 한다.

검증 범위:

1. Architecture test
2. Entity-Flyway alignment test
3. Agent dispatch test
4. Agent suggestion controller/service test
5. Semantic search 권한 test
6. Room membership integration test
7. 전체 `gradlew test`

완료 기준:

```powershell
.\gradlew.bat test --console=plain
```

위 명령이 통과해야 한다.

상태: 검증 완료

2026-06-30 기준 아래 검증이 통과했다.

```powershell
.\gradlew.bat test --tests com.bubli.architecture.ArchitectureTest --tests com.bubli.architecture.DomainDependencyArchitectureTest --tests com.bubli.schema.EntityFlywayAlignmentTest --tests com.bubli.EntityMappingTest --tests com.bubli.agent.dispatch.* --tests com.bubli.agent.service.* --tests com.bubli.resource.service.ResourceRelationIndexPublicServiceTest --tests com.bubli.memory.service.DailySummaryServiceTest --console=plain
.\gradlew.bat test --console=plain
```

### 권장 구현 순서

1. Step 6.7: resource relation 자동 생성 완료
2. Step 6.8: Postman E2E 문서화 완료
3. Step 6.9: 전체 테스트와 schema 정합성 검증 완료
