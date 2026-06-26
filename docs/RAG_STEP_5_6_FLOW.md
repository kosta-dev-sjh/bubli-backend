# RAG Step 5, Step 5.5, Step 6 구현 흐름

이 문서는 현재 코드 기준으로 Step 5와 Step 6이 어떻게 동작하는지 설명한다.

- Step 5: `resource_embeddings` 기반 인덱싱과 semantic search
- Step 5.5: 페이지 추적, 검색 권한 검증, 개인 검색, vector formatter 정리
- Step 6: `agent_suggestions` 후보 조회와 승인/보류/거절/수정

## 전체 흐름

```text
문서 업로드
-> Resource / ResourceFile / ResourceVersion 저장
-> AgentJob(ANALYZE_RESOURCE, PENDING) 생성
-> AnalyzeResourceJobService.process(jobId)
-> ResourceAnalysisPublicService.analyzeResourceForJob(resourceId, jobId)
-> PDF/TXT 텍스트 추출
-> ResourceSummary 저장
-> AiDocument 저장
-> ResourceEmbeddingIndexPublicService.index(...)
-> TextChunker로 chunk 생성
-> EmbeddingModel 호출
-> resource_embeddings 저장
-> POST /api/ai/search-resource 로 검색
```

Step 6은 위 흐름에서 생성된 `AgentJob` 또는 AI 분석 결과와 연결되는 후보 관리 기능이다.

```text
AgentAnalysisResult.suggestions
-> AgentSuggestionCommandService.createDrafts(...)
-> agent_suggestions DRAFT 저장
-> GET /api/agent/suggestions
-> PATCH /api/agent/suggestions/{suggestionId}
-> APPROVED / HELD / REJECTED / MODIFY
```

현재 `AgentAnalysisResult`를 실제 LLM 호출 결과로 받아 `createDrafts(...)`까지 자동 연결하는 HTTP 흐름은 아직 없다. Step 6의 저장/조회/검토 기능은 서비스와 API로 구현되어 있다.

## Step 5: Resource Embedding 인덱싱

### 핵심 테이블

`resource_embeddings`는 검색 가능한 chunk와 vector를 저장한다.

주요 컬럼:

| 컬럼 | 의미 |
|---|---|
| `resource_id` | 원본 자료 ID |
| `owner_id` | 개인 검색 권한 기준 사용자 |
| `room_id` | 프로젝트룸 검색 권한 기준 |
| `visibility` | `PERSONAL` 또는 `ROOM_SHARED` |
| `chunk_index` | 자료 내 chunk 순서 |
| `chunk_text` | 검색 결과로 반환할 원문 일부 |
| `embedding` | pgvector `vector(1024)` |
| `chunk_metadata` | 파일명, MIME type, pageNumber, offset 등 |

### `ResourceAnalysisPublicService`

파일: `src/main/java/com/bubli/resource/service/ResourceAnalysisPublicService.java`

역할:

- resource 도메인의 분석 흐름을 소유한다.
- agent 도메인이 resource repository/entity에 직접 접근하지 않도록 public service 경계 역할을 한다.
- `Resource`, `ResourceFile`, `ResourceSummary`, `AiDocument`를 처리한다.
- PDF/TXT에서 텍스트를 추출하고 embedding indexing으로 넘긴다.

중요 메서드:

```java
public void analyzeResourceForJob(UUID resourceId, UUID jobId)
```

처리 순서:

1. `ResourceRepository.findById(resourceId)`로 자료 조회
2. `resource.startAnalysis()`
3. 최신 `ResourceFile` 조회
4. `extract(resourceFile)`로 텍스트 추출
5. 추출 텍스트가 비어 있으면 실패 처리
6. `ResourceSummary.analyzed(...)` 저장
7. `AiDocument.analyzed(...)` 저장 또는 기존 문서 재사용
8. `ResourceEmbeddingIndexPublicService.index(...)` 호출
9. `resource.markAnalyzed()`
10. 예외 발생 시 `resource.markAnalysisFailed()`

### PDF 페이지 추적

Step 5.5에서 PDF 추출 방식이 바뀌었다.

기존:

```text
PDF 전체 텍스트를 한 번에 추출
-> chunk가 어느 페이지에서 왔는지 알 수 없음
```

현재:

```text
PDF page 1 텍스트 추출
PDF page 2 텍스트 추출
...
-> TextPage(pageNumber, text) 목록 생성
-> 페이지 단위 chunking
-> chunk_metadata.pageNumber 저장
```

관련 코드:

```java
private ExtractedDocument extractPdf(InputStream inputStream)
```

PDFBox의 `PDFTextStripper`를 페이지별로 실행한다.

```java
stripper.setStartPage(pageNumber);
stripper.setEndPage(pageNumber);
pages.add(new TextChunker.TextPage(pageNumber, stripper.getText(document)));
```

TXT는 페이지 개념이 없으므로 `pageNumber = null`로 처리한다.

### `TextChunker`

파일: `src/main/java/com/bubli/resource/service/TextChunker.java`

역할:

- 긴 텍스트를 검색 가능한 chunk로 나눈다.
- chunk 크기는 최대 1200자, overlap은 200자다.
- Step 5.5부터 페이지 단위 chunking을 지원한다.

기존 호환 메서드:

```java
public List<TextChunk> split(String text)
```

새 메서드:

```java
public List<TextChunk> splitPages(List<TextPage> pages)
```

`TextPage`:

```java
public record TextPage(
        Integer pageNumber,
        String text
) {}
```

`TextChunk`:

```java
public record TextChunk(
        int index,
        String text,
        int startOffset,
        int endOffset,
        Integer pageNumber
) {}
```

설계 의도:

- PDF chunk가 페이지 경계를 넘지 않는다.
- `chunk_index`는 전체 문서 기준으로 0부터 증가한다.
- `pageNumber`는 검색 결과 근거 표시와 원문 추적에 사용한다.

### `ResourceEmbeddingIndexPublicService`

파일: `src/main/java/com/bubli/resource/service/ResourceEmbeddingIndexPublicService.java`

역할:

- resource 텍스트를 chunk로 나눈다.
- chunk마다 embedding을 생성한다.
- `resource_embeddings`에 저장한다.

주요 메서드:

```java
public IndexResult index(Resource resource, ResourceFile resourceFile, String text)
```

기존 호환용이다. 내부적으로 `TextPage(null, text)`로 변환한다.

```java
public IndexResult index(Resource resource, ResourceFile resourceFile, List<TextChunker.TextPage> pages)
```

Step 5.5의 실제 page-aware indexing 메서드다.

처리 순서:

1. `ObjectProvider<EmbeddingModel>`에서 embedding model 조회
2. 없으면 `IndexResult.skipped()`
3. `TextChunker.splitPages(pages)` 호출
4. 기존 embedding 삭제: `deleteAllByResourceId(resource.getId())`
5. chunk별 `embeddingModel.embed(chunk.text())` 호출
6. `EmbeddingVectorFormatter.toVectorLiteral(...)`로 pgvector literal 생성
7. `ResourceEmbedding.create(...)`
8. `resourceEmbeddingRepository.saveAll(...)`

저장되는 metadata:

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

### `EmbeddingVectorFormatter`

파일: `src/main/java/com/bubli/resource/service/EmbeddingVectorFormatter.java`

역할:

- embedding vector를 PostgreSQL pgvector literal 문자열로 변환한다.
- indexing/search 양쪽의 중복 검증 로직을 제거한다.

검증:

- vector가 `null`이면 실패
- dimension이 1024가 아니면 실패
- `NaN`, `Infinity`가 있으면 실패

출력 예:

```text
[0.1,0.2,0.3,...]
```

이 문자열은 native query에서 다음처럼 사용된다.

```sql
CAST(:queryEmbedding AS vector)
```

## Step 5.5: Semantic Search 개선

### 검색 API

파일:

- `src/main/java/com/bubli/agent/controller/AgentJobController.java`
- `src/main/java/com/bubli/agent/dto/SearchResourceRequest.java`
- `src/main/java/com/bubli/agent/dto/SearchResourceResponse.java`

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

`scope`를 생략하면 `ROOM_SHARED`가 기본값이다.

### `ResourceSearchScope`

파일: `src/main/java/com/bubli/resource/type/ResourceSearchScope.java`

```java
public enum ResourceSearchScope {
    ROOM_SHARED,
    PERSONAL
}
```

검색 범위를 명시한다.

- `ROOM_SHARED`: 프로젝트룸 공유 자료 검색
- `PERSONAL`: 현재 사용자 개인 자료 검색

### `ResourceSemanticSearchPublicService`

파일: `src/main/java/com/bubli/resource/service/ResourceSemanticSearchPublicService.java`

역할:

- 검색 query를 embedding으로 변환한다.
- scope에 따라 repository 검색 메서드를 선택한다.
- `ROOM_SHARED` 검색 시 room membership을 검증한다.
- 검색 row를 API 응답 DTO로 변환한다.

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

1. `userId` 필수 검증
2. `query` blank 검증
3. embedding model 조회
4. query embedding 생성
5. `EmbeddingVectorFormatter.toVectorLiteral(...)`
6. `topK` 보정
   - null이면 5
   - 1보다 작으면 1
   - 20보다 크면 20
7. scope 분기
   - `PERSONAL`: `searchPersonal(userId, ...)`
   - `ROOM_SHARED`: roomId 필수, membership 검증 후 `searchRoomShared(roomId, ...)`

### 권한 검증

파일:

- `src/main/java/com/bubli/project/service/ProjectRoomAccessPublicService.java`
- `src/main/java/com/bubli/project/repository/RoomMemberRepository.java`

`ROOM_SHARED` 검색은 반드시 현재 사용자가 해당 room의 ACTIVE member인지 확인한다.

```java
projectRoomAccessService.requireRoomMember(roomId, userId);
```

`RoomMemberRepository`는 현재 `RoomMember` 엔티티가 placeholder에 가까우므로 native query를 사용한다.

```sql
SELECT COUNT(*) > 0
FROM room_members
WHERE room_id = :roomId
  AND user_id = :userId
  AND status = 'ACTIVE'
```

실패 시:

```java
throw new BusinessException(ErrorCode.PROJECT_403_001);
```

아키텍처 관점:

- resource 도메인은 project repository/entity를 직접 보지 않는다.
- resource 도메인은 `ProjectRoomAccessPublicService`라는 public service에만 의존한다.
- ArchitectureTest 규칙을 만족한다.

### `ResourceEmbeddingRepository`

파일: `src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java`

ROOM_SHARED 검색:

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

PERSONAL 검색:

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

유사도:

```sql
1 - (embedding <=> CAST(:queryEmbedding AS vector)) AS similarityScore
```

### 검색 응답

파일: `src/main/java/com/bubli/resource/dto/ResourceSearchHit.java`

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

`pageNumber`는 `chunk_metadata` JSON에서 파싱해서 별도 필드로 노출한다.

예:

```json
{
  "embeddingId": "uuid",
  "resourceId": "uuid",
  "chunkIndex": 0,
  "chunkText": "로그인 기능은 이메일과 비밀번호로...",
  "pageNumber": 2,
  "chunkMetadata": "{\"pageNumber\":2,\"originalName\":\"requirements.pdf\"}",
  "similarityScore": 0.83
}
```

## Step 6: Agent Suggestion

Step 6은 AI가 만든 후보를 저장하고, 사용자가 검토 상태를 변경하는 기능이다.

### 핵심 테이블

`agent_suggestions`

주요 컬럼:

| 컬럼 | 의미 |
|---|---|
| `user_id` | 후보 소유 사용자 |
| `room_id` | 프로젝트룸 후보인 경우 room |
| `job_id` | 후보를 만든 agent job |
| `resource_id` | 근거 자료 |
| `suggestion_type` | `TASK`, `REQUIREMENT`, `REVIEW_ITEM` 등 |
| `payload_json` | 실제 후보 내용 |
| `evidence_json` | 근거 정보 |
| `status` | `DRAFT`, `APPROVED`, `HELD`, `REJECTED` |
| `reviewed_by` | 검토자 |
| `reviewed_at` | 검토 시각 |

### `AgentSuggestion`

파일: `src/main/java/com/bubli/agent/entity/AgentSuggestion.java`

역할:

- suggestion의 상태와 payload/evidence를 소유한다.
- draft 생성과 상태 전이를 책임진다.

생성:

```java
AgentSuggestion.draft(
        userId,
        roomId,
        jobId,
        resourceId,
        suggestionType,
        payloadJson,
        evidenceJson
)
```

상태 변경:

```java
approve(reviewerId)
hold(reviewerId)
reject(reviewerId)
modify(reviewerId, modifiedPayloadJson)
```

규칙:

- `DRAFT` 상태에서만 변경 가능
- 이미 `APPROVED`, `HELD`, `REJECTED`가 된 suggestion은 다시 변경할 수 없다.
- `modify(...)`는 상태를 유지하면서 payload를 교체하고 `reviewedBy`, `reviewedAt`을 기록한다.

### `AgentSuggestionCommandService`

파일: `src/main/java/com/bubli/agent/service/AgentSuggestionCommandService.java`

역할:

- suggestion 검토 명령 처리
- AI 분석 결과를 draft suggestion으로 변환 저장

검토 메서드:

```java
public AgentSuggestionResponse review(
        UUID suggestionId,
        UUID reviewerId,
        AgentSuggestionReviewAction action,
        Map<String, Object> payloadJson
)
```

지원 action:

```java
APPROVE
HOLD
REJECT
MODIFY
```

처리:

- suggestion이 없으면 `AGENT_404_002`
- 잘못된 상태 전이 또는 payload 누락은 `COMMON_400_002`

AI 분석 결과 저장 메서드:

```java
public List<AgentSuggestionResponse> createDrafts(
        UUID userId,
        UUID roomId,
        UUID jobId,
        UUID resourceId,
        AgentAnalysisResult result
)
```

`AgentAnalysisResult.suggestions()`를 `AgentSuggestion` 목록으로 변환한다.

type mapping:

| contract type | 저장 type |
|---|---|
| `TASK` | `TASK` |
| `REQUIREMENT` | `REQUIREMENT` |
| `CONTRACT_FIELD` | `REVIEW_ITEM` |

payload 예:

```json
{
  "type": "TASK",
  "title": "로그인 API 구현",
  "description": "JWT 기반 로그인 API를 구현한다.",
  "fieldKey": null,
  "value": null,
  "confidence": 0.9
}
```

evidence 예:

```json
{
  "resourceId": "uuid",
  "sourceText": "계약서 원문 일부",
  "modelName": "test-model",
  "promptVersion": "prompt-v1"
}
```

### `AgentSuggestionQueryService`

파일: `src/main/java/com/bubli/agent/service/AgentSuggestionQueryService.java`

역할:

- 내 suggestion 목록 조회
- room suggestion 목록 조회
- status/type 필터 적용

내 suggestion:

```java
findMine(userId, status, suggestionType)
```

room suggestion:

```java
findRoomSuggestions(roomId, status, suggestionType)
```

현재 room suggestion 조회에도 room membership 검증은 아직 붙어 있지 않다. Step 5.5에서 만든 `ProjectRoomAccessPublicService`를 Step 6 query에도 연결하는 것이 다음 보강 지점이다.

### `AgentSuggestionController`

파일: `src/main/java/com/bubli/agent/controller/AgentSuggestionController.java`

내 suggestion 조회:

```http
GET /api/agent/suggestions?status=DRAFT&suggestionType=TASK
Authorization: Bearer {accessToken}
```

room suggestion 조회:

```http
GET /api/project-rooms/{roomId}/agent/suggestions?status=DRAFT
Authorization: Bearer {accessToken}
```

suggestion 검토:

```http
PATCH /api/agent/suggestions/{suggestionId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "action": "APPROVE"
}
```

수정:

```http
PATCH /api/agent/suggestions/{suggestionId}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "action": "MODIFY",
  "payloadJson": {
    "title": "로그인 API 구현",
    "description": "JWT 기반 로그인 API를 구현한다.",
    "priority": "HIGH"
  }
}
```

### `AgentSuggestionResponse`

파일: `src/main/java/com/bubli/agent/dto/AgentSuggestionResponse.java`

응답 필드:

```java
UUID suggestionId
UUID userId
UUID roomId
UUID jobId
UUID resourceId
AgentSuggestionType suggestionType
AgentSuggestionStatus status
Map<String, Object> payloadJson
Map<String, Object> evidenceJson
UUID reviewedBy
Instant reviewedAt
Instant createdAt
Instant updatedAt
```

## Postman 검증 흐름

### 사전 조건

모든 `/api/**` 요청은 JWT가 필요하다.

현재 auth API가 아직 구현되어 있지 않으므로 테스트용 JWT를 직접 만들어야 한다.

또한 Step 5 semantic search는 embedding model이 필요하므로 `local,ai` profile과 Bedrock credential이 필요하다.

### Step 5 검증

1. 문서 업로드

```http
POST /api/project-rooms/{roomId}/contract-documents
```

form-data:

```text
documentType = REQUIREMENT
file = PDF 또는 TXT
```

2. 분석 job 생성 확인

응답의 `jobId`, `resourceId` 저장

3. job 처리

현재 `AnalyzeResourceJobService.process(jobId)`를 직접 호출하는 dev endpoint는 없다.
Postman으로 완전한 E2E를 하려면 dev/test 전용 endpoint를 별도로 추가해야 한다.

4. 검색

ROOM_SHARED:

```http
POST /api/ai/search-resource

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

{
  "scope": "PERSONAL",
  "query": "개인 자료 검색",
  "topK": 5
}
```

검증 포인트:

- ROOM_SHARED 검색은 `room_members`에 ACTIVE row가 없으면 403
- PERSONAL 검색은 `owner_id = currentUser.userId()`만 검색
- PDF 검색 결과는 `pageNumber`가 포함되어야 함
- `chunkMetadata`에도 `pageNumber`가 포함되어야 함

### Step 6 검증

현재 Step 6은 DB seed 후 Postman으로 바로 테스트할 수 있다.

테스트 suggestion seed 예:

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
    uuid_generate_v4(),
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

{
  "action": "APPROVE"
}
```

수정:

```http
PATCH /api/agent/suggestions/{suggestionId}

{
  "action": "MODIFY",
  "payloadJson": {
    "title": "로그인 API 구현",
    "description": "JWT 기반 로그인 API 구현",
    "priority": "HIGH"
  }
}
```

## 테스트

현재 관련 테스트:

- `EmbeddingVectorFormatterTest`
- `TextChunkerTest`
- `ResourceEmbeddingIndexPublicServiceTest`
- `ResourceSemanticSearchPublicServiceTest`
- `AgentSuggestionCommandServiceTest`
- `AgentSuggestionQueryServiceTest`
- `ArchitectureTest`

검증 명령:

```powershell
.\gradlew.bat test
```

현재 전체 테스트는 통과한다.

## 남은 보강 지점

1. `ANALYZE_RESOURCE` job 처리용 dev/test endpoint 추가
   - Postman E2E 검증용
   - 운영 profile에서는 비활성화 필요

2. Step 6 room suggestion 조회 권한 검증
   - `AgentSuggestionQueryService.findRoomSuggestions(...)`에 `ProjectRoomAccessPublicService` 연결

3. LLM 분석 결과와 Step 6 자동 연결
   - `AgentAnalysisResultJsonParser`
   - `AgentSuggestionCommandService.createDrafts(...)`
   - `AnalyzeResourceJobService` 또는 별도 agent generation job과 연결

4. 승인된 suggestion의 원본 도메인 반영
   - `TASK` 승인 시 work/task 생성
   - `WBS` 승인 시 work/wbs 생성
   - 현재는 suggestion 상태 변경까지만 구현되어 있음
