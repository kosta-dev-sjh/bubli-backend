# RAG Step 3 구현

## 범위

- 프로젝트룸 PDF/TXT multipart 업로드 API
- PDF magic bytes 및 UTF-8 TXT 검증
- SHA-256 체크섬 기반 동일 프로젝트룸 중복 차단
- 안전한 상대 경로를 사용하는 로컬 원본 저장
- 명세 변경에 맞춰 `Resource`, `ResourceFile`, `ResourceVersion` 생성
- 후속 분석 작업용 `AgentJob(ANALYZE_RESOURCE)` 생성
- DB 트랜잭션 롤백 시 저장 파일 보상 삭제

## API

```http
POST /api/project-rooms/{projectRoomId}/contract-documents
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

multipart 필드:

| 필드 | 형식 | 설명 |
|---|---|---|
| `documentType` | enum | `CONTRACT`, `REQUIREMENT` |
| `file` | binary | 최대 50MB PDF 또는 UTF-8 TXT |

성공 시 HTTP 201과 다음 식별자를 반환한다.

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

## 저장 경로

원본 파일명은 메타데이터에만 보존하고 실제 키에는 사용하지 않는다.

```text
resources/{projectRoomId}/{yyyy}/{MM}/{randomUuid}.{pdf|txt}
```

로컬 프로필의 실제 기준 디렉터리는 `storage.local.base-path`이며 기본값은
`./local-storage`다.

## 다음 단계

Step 4에서는 `ANALYZE_RESOURCE` AgentJob을 소비해 자료 요약, 임베딩, AI 문서 분류를 처리한다.
