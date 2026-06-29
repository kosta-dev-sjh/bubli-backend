# RAG Step 3 구현

## 범위

- 프로젝트룸 PDF/TXT multipart 업로드 API
- PDF magic bytes 및 UTF-8 TXT 검증
- SHA-256 checksum 기반 동일 프로젝트룸 중복 차단
- 안전한 상대 storage key를 사용한 로컬 원본 저장
- API/DB 명세에 맞춰 `Resource`, `ResourceFile`, `ResourceVersion` 생성
- 후속 분석 작업으로 `AgentJob(ANALYZE_RESOURCE)` 생성
- DB 트랜잭션 롤백 시 저장 파일 보상 삭제

## API

```http
POST /api/project-rooms/{roomId}/contract-documents
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

multipart 필드:

| 필드 | 형식 | 설명 |
|---|---|---|
| `documentType` | enum | `CONTRACT`, `REQUIREMENT` |
| `file` | binary | 최대 50MB PDF 또는 UTF-8 TXT |

성공 시 HTTP 201과 다음 payload를 반환한다.

```json
{
  "success": true,
  "data": {
    "resourceId": "uuid",
    "jobId": "uuid",
    "status": "PENDING"
  },
  "error": null
}
```

## 저장 구조

저장 대상은 구형 `documents` 테이블이 아니라 API/Data Model 기준의 자료 테이블이다.

```text
resources
resource_files
resource_versions
agent_jobs
```

파일 저장 key 예:

```text
resources/{roomId}/{yyyy}/{MM}/{randomUuid}.{pdf|txt}
```

## 다음 단계

Step 4에서 `ANALYZE_RESOURCE` 작업을 처리해 `resource_summaries`와 `ai_documents`를 생성한다.
