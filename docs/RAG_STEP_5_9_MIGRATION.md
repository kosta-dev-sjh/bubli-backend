# RAG Step 5/9 이관 정리

## 목표

최종 기준은 `09_Data-Model.md`와 `10_API-Design_revised.md`다.

따라서 Step 5는 구형 `DocumentChunk` 기반 인덱싱이 아니라 `resource_embeddings` 기반 인덱싱으로 구현한다. Step 9는 구형 Step 2 모델을 새 모델로 흡수하고 제거하는 정리 단계다.

## Step 9: legacy 모델 정리

제거한 Java 모델:

| legacy | 대체 기준 |
|---|---|
| `Document` | `Resource`, `ResourceFile`, `ResourceVersion`, `AiDocument` |
| `DocumentChunk` | `ResourceEmbedding` |
| `AgentRequest` | `AgentJob` |
| `SuggestionEvidence` | `AgentSuggestion.evidenceJson` |

DB는 기존 Flyway 적용 이력을 고려해 V2/V3 파일을 직접 수정하지 않고, 새 V5 마이그레이션에서 legacy 테이블을 제거한다.

```text
suggestion_evidences
agent_requests
document_chunks
documents
```

## Step 5: resource_embeddings 기반 인덱싱

새 저장 대상:

```text
resource_embeddings
```

컬럼 기준:

| 컬럼 | 설명 |
|---|---|
| `resource_id` | 원본 자료 ID |
| `owner_id` | 개인 자료 권한 필터 |
| `room_id` | 프로젝트룸 자료 권한 필터 |
| `visibility` | `PERSONAL`, `ROOM_SHARED` |
| `chunk_index` | 자료 안 chunk 순서 |
| `chunk_text` | 검색 대상 원문 |
| `embedding` | Titan Embedding V2 기준 `vector(1024)` |
| `chunk_metadata` | 페이지, 섹션 등 부가정보 JSON |

인덱스:

```text
UNIQUE(resource_id, chunk_index)
HNSW embedding vector_cosine_ops
resource_id
owner_id, visibility
room_id, visibility
```

## API 영향

API는 `Document`나 `AgentRequest`를 노출하지 않는다.

사용하는 API:

```http
POST /api/project-rooms/{roomId}/contract-documents
POST /api/ai/analyze-resource
GET /api/agent-jobs/{jobId}
POST /api/ai/search-resource
GET /api/agent/suggestions
GET /api/project-rooms/{roomId}/agent/suggestions
PATCH /api/agent/suggestions/{id}
```

현재 구현된 범위:

- `resource_embeddings` DB/Entity/Repository 추가
- legacy RAG DB 테이블 드롭 마이그레이션 추가
- legacy Java 엔티티/Repository/테스트 제거
- `POST /api/ai/analyze-resource` 추가
- `GET /api/agent-jobs/{jobId}`의 `suggestionIds`를 `agent_suggestions.job_id` 기준으로 보정

남은 범위:

- 텍스트 chunker 구현
- Titan Embedding 호출 연결
- `resource_embeddings.embedding` 저장 방식 확정
- 권한 필터가 적용된 semantic search 구현
- `POST /api/ai/search-resource` 구현
