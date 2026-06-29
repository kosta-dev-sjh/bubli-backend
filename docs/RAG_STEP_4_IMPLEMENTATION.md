# RAG Step 4 구현

## 기준

Step 4는 구형 Step 2의 `AgentRequest`/`Document` 중심 흐름을 사용하지 않는다.

API 명세와 `09_Data-Model.md` 기준에 맞춰 `AgentJob(ANALYZE_RESOURCE)`를 부모 작업으로 두고, 자료 분석 결과를 `resource_summaries`, `ai_documents`에 저장한다.

## 처리 흐름

```text
AgentJob(PENDING, ANALYZE_RESOURCE)
-> AgentJob.start()
-> ResourceFile.storageKey에서 원본 파일 읽기
-> PDF/TXT 텍스트 추출
-> resource_summaries 저장
-> ai_documents 저장
-> Resource.status = ANALYZED
-> AgentJob.status = SUCCEEDED
```

실패 시:

```text
Resource.status = FAILED
AgentJob.status = FAILED
AgentJob.errorCode = RESOURCE_ANALYSIS_FAILED
AgentJob.errorMessage = 실패 원인
```

## API

기존 자료에 대해 분석 작업을 새로 만들 수 있다.

```http
POST /api/ai/analyze-resource
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "resourceId": "uuid"
}
```

작업 상태 조회:

```http
GET /api/agent-jobs/{jobId}
```

`AgentJobResponse`는 API 명세 기준을 따른다.

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

`suggestionIds`는 `agent_suggestions.job_id` 기준으로 조회한다.

## 현재 범위

- PDF 텍스트 추출은 PDFBox 사용
- TXT 텍스트 추출은 UTF-8 사용
- 요약은 아직 LLM 호출 없이 추출 텍스트 preview와 기본 메타데이터를 JSON으로 저장
- 문서 타입은 파일명/텍스트 기반 heuristic으로 분류
- 실제 임베딩 및 `resource_embeddings` 저장은 Step 5에서 처리
