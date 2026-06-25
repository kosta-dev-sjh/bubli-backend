# RAG Step 4 구현

## 기준

변경된 DB/API 명세를 우선한다.

따라서 Step 4는 기존 Step 2의 `AgentRequest`/`Document` 중심 흐름이 아니라,
Step 3에서 생성한 `AgentJob(ANALYZE_RESOURCE)`를 기준으로 처리한다.

## 처리 흐름

```text
AgentJob(PENDING, ANALYZE_RESOURCE)
→ AgentJob.start()
→ ResourceFile.storagePath에서 원본 파일 읽기
→ PDF/TXT 텍스트 추출
→ resource_summaries 저장
→ ai_documents 저장
→ Resource.status = ANALYZED
→ AgentJob.status = SUCCEEDED
```

실패 시:

```text
Resource.status = FAILED
AgentJob.status = FAILED
AgentJob.errorCode = RESOURCE_ANALYSIS_FAILED
AgentJob.errorMessage = 실패 원인
```

## 구현 파일

| 파일 | 역할 |
|---|---|
| `AnalyzeResourceJobService` | `ANALYZE_RESOURCE` 작업 처리 |
| `AgentJobQueryService` | `GET /api/agent-jobs/{jobId}` 응답 구성 |
| `AgentJobController` | 작업 상태 조회 API |
| `ResourceSummary` | `resource_summaries` 엔티티 |
| `AiDocument` | `ai_documents` 엔티티 |
| `FileStorage.open()` | 저장된 원본 파일 읽기 |

## 현재 범위

- PDF 텍스트 추출은 PDFBox를 사용한다.
- TXT 텍스트 추출은 UTF-8로 읽는다.
- 요약은 외부 LLM 호출 없이 추출 텍스트의 앞부분과 기본 메타데이터를 JSON으로 저장한다.
- 문서 타입은 현재 파일명/텍스트 기반 휴리스틱으로 분류한다.
- 실제 임베딩 및 `resource_embeddings` 저장은 다음 단계로 둔다.

## API

```http
GET /api/agent-jobs/{jobId}
```

명세의 `AgentJobResponse` 구조에 맞춰 다음 값을 반환한다.

```json
{
  "jobId": "uuid",
  "jobType": "ANALYZE_RESOURCE",
  "status": "SUCCEEDED",
  "resourceId": "uuid",
  "roomId": "uuid",
  "errorCode": null,
  "errorMessage": null,
  "retryCount": 0,
  "suggestionIds": [],
  "resourceSummaryId": "uuid",
  "aiDocumentId": "uuid",
  "startedAt": "2026-06-25T00:00:00Z",
  "finishedAt": "2026-06-25T00:00:01Z"
}
```
