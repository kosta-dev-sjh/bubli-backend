현재 구현은 MVP 수준에서는 꽤 탄탄합니다. 권한 범위 분리, pgvector/HNSW, 문서 청크, 제목 기반 보완 검색, 인용 메타데이터까지 있습니다. 다만 검색 정확도를 측정하거나 검색 결과를 정교하게 재정렬하는 부분이 부족해, 데이터가 많아질수록 품질과 성능 편차가 커질 구조입니다.

## 현재 검색 구조

프로젝트룸 문서 검색은 대략 다음 순서입니다.

1. 사용자 질문에서 문서·작업·일정 등의 검색 의도 추출
2. semantic search 실행
3. keyword search 실행
4. 최근 문서 제목 최대 30개에서 제목 매칭
5. 제목에 매칭된 문서만 대상으로 semantic/keyword search 재실행
6. 결과를 중복 제거해 병합
7. threshold를 통과한 청크를 LLM 프롬프트에 추가
8. 문서명·페이지·줄·offset·유사도 정보를 citation으로 반환

실제 메인 경로는 [ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:63)입니다.

## 주요 한계점

### 1. Hybrid search가 아니라 단순 결과 병합에 가깝습니다

Semantic 결과와 keyword 결과를 embedding ID 기준으로 합칠 뿐, 두 점수를 통합해서 다시 순위를 계산하지 않습니다.

- Semantic 점수: cosine similarity
- Keyword 점수: 포함된 키워드 수 ÷ 전체 키워드 수
- 두 점수의 의미와 분포가 전혀 다른데 그대로 섞입니다.
- 제목 매칭이 없으면 semantic 결과 다음에 keyword 결과가 붙는 순서가 됩니다.
- RRF, weighted score, reranker 같은 최종 재정렬 과정이 없습니다.

관련 병합 코드는 [ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:441)입니다.

따라서 keyword에서 매우 정확하게 일치한 청크가 semantic의 애매한 결과보다 뒤로 밀릴 수 있습니다.

### 2. Keyword search의 정확도와 성능이 모두 제한적입니다

현재 keyword 검색은 다음 형태입니다.

```sql
lower(chunk_text) LIKE '%keyword%'
```

[ResourceEmbeddingRepository.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java:116)

한계는 다음과 같습니다.

- `%키워드%` 검색이라 일반 B-tree 인덱스를 사용할 수 없습니다.
- 프로젝트룸 문서가 많아지면 청크 텍스트를 대량 스캔하게 됩니다.
- 형태소 분석, 어간 처리, 오타 교정, 동의어 처리가 없습니다.
- 정확한 단어가 아니라 부분 문자열만 포함해도 매칭됩니다.
- TF-IDF/BM25가 아니라 키워드 포함 여부만 0/1로 계산합니다.
- 키워드는 최대 5개로 잘립니다.
- 1글자 키워드는 제거되어 약어·제품 코드·한 글자 요구사항을 놓칠 수 있습니다.
- 단어 빈도, 등장 위치, 키워드 간 거리, 제목 가중치가 없습니다.

PostgreSQL FTS의 `tsvector/tsquery`, `pg_trgm`, 혹은 별도 BM25 검색을 적용할 필요가 있습니다.

### 3. 검색 결과의 출처 유형이 잘못 기록될 수 있습니다

Semantic과 keyword 결과를 병합한 뒤 모든 문서 근거에 다음 값을 넣습니다.

```java
metadata.put("retrievalMode", "SEMANTIC");
```

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:520)

따라서 실제로 keyword fallback으로 찾은 결과도 API citation에서는 `SEMANTIC`으로 표시됩니다. 대표 청크 fallback도 동일하게 semantic 결과처럼 보일 수 있습니다.

이 때문에 운영 중 “어떤 검색 전략으로 찾은 문서인지” 분석하기 어렵고, 검색 품질 지표도 왜곡됩니다.

### 4. Top-K 계산과 실제 제한이 충돌합니다

제목 매칭 문서가 여러 개면 다음 계산으로 검색량을 늘리려 합니다.

```java
baseTopK * resourceCount * 3
```

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:457)

하지만 실제 검색 서비스는 모든 `topK`를 최대 20으로 제한합니다.

[ResourceSemanticSearchPublicService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/service/ResourceSemanticSearchPublicService.java:236)

예를 들어 제목 매칭 문서가 3개라서 45개를 요청해도 실제로는 20개만 검색됩니다. 문서별 최소 결과 수도 보장하지 않아 한 문서의 청크가 결과를 대부분 차지할 수 있습니다.

### 5. 문서 다양성 제어가 없습니다

검색 단위가 청크이므로 같은 문서의 서로 인접한 청크가 Top-K를 전부 차지할 수 있습니다.

현재 없는 기능은 다음과 같습니다.

- 문서별 최대 청크 개수
- 인접·중복 청크 제거
- MMR 기반 다양성 확보
- 같은 문서의 연속 청크 병합
- 문서 단위 점수와 청크 단위 점수의 조합

1200자 청크에 200자 overlap이 있어 인접 청크의 내용이 중복되기 때문에 이 문제가 더 쉽게 발생합니다.

### 6. Chunking 기준이 고정된 글자 수입니다

[TextChunker.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/service/TextChunker.java:23)에서 1200자, overlap 200자로 고정되어 있습니다.

- 모델의 실제 토큰 수를 기준으로 하지 않습니다.
- 표, 목록, 계약 조항, 요구사항 번호 구조를 인식하지 않습니다.
- 한국어와 일본어 문장 경계를 충분히 인식하지 못합니다.
- PDF 페이지마다 별도로 청크를 나누므로 페이지를 가로지르는 문맥이 끊깁니다.
- 공백을 정규화한 뒤 offset을 계산하므로 원본 문서 위치와 offset이 정확히 일치하지 않을 수 있습니다.
- 짧은 표 행이나 제목이 본문과 분리될 수 있습니다.

문서 종류별 chunking 전략과 heading/table-aware chunking이 필요합니다.

### 7. 고정 similarity threshold에 의존합니다

운영 기본값은 일반 답변 `0.72`, 제안 `0.68`, 개인 검색 `0.72`입니다.

[application-prod.yml](D:/kostaEx/bubli-backend/src/main/resources/application-prod.yml:166)

하지만 threshold가 다음 조건을 고려하지 않습니다.

- 질문 길이와 구체성
- 한국어·영어·일본어 차이
- 문서 종류
- 문서 개수
- embedding 모델 변경
- 질문이 키워드형인지 설명형인지
- Top-1과 Top-2 점수 차이

고정 `0.72`가 어떤 질문에는 너무 높아 결과가 없고, 어떤 질문에는 낮아서 관련 없는 청크가 포함될 수 있습니다. 현재 threshold를 검증할 정답 corpus도 없습니다.

### 8. Query 이해가 규칙 기반입니다

문서 검색 여부, 제목 토큰, 요구사항 ID, 일정·작업 의도를 문자열 포함 규칙으로 판단합니다.

[AgentQuerySupport.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/AgentQuerySupport.java:48)

- 등록되지 않은 표현은 문서 검색 자체가 실행되지 않을 수 있습니다.
- 다국어 표현 확장 시 조건문이 계속 커집니다.
- 복합 질문에서 어떤 데이터 소스를 검색해야 하는지 잘못 판단할 수 있습니다.
- query expansion, synonym expansion, multi-query retrieval이 없습니다.
- 대화 문맥을 활용한 후속 질문 재작성도 검색 단계에서는 부족합니다.

예를 들어 “그 문서에서 결제 조건은?” 같은 후속 질문은 이전에 지칭한 문서를 안정적으로 특정하기 어렵습니다.

### 9. 제목 검색 범위가 최근 30개 문서뿐입니다

제목 매칭은 프로젝트룸의 최근 자료 30개만 불러와 수행합니다.

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:366)

오래된 문서가 명확하게 이름으로 언급돼도 30개 밖이면 제목 기반 검색에 걸리지 않습니다. 또한 각 자료의 요약을 개별 조회하고, 검색 결과의 제목도 다시 개별 조회해 N+1 쿼리가 발생할 가능성이 있습니다.

### 10. 대표 청크 fallback이 문서별 대표성을 보장하지 않습니다

대표 청크 조회는 다음과 같이 정렬합니다.

```sql
ORDER BY resource_id ASC, chunk_index ASC
LIMIT :limit
```

[ResourceEmbeddingRepository.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java:204)

여러 문서를 조회하면 UUID 정렬상 먼저 오는 문서의 앞부분이 결과를 대부분 차지할 수 있습니다. 문서별 첫 청크 1개를 가져오는 방식이 아니며, 반환 점수도 무조건 `1.0`이라 실제 semantic similarity처럼 오해될 수 있습니다.

### 11. 개인 자료 검색은 semantic search만 있습니다

프로젝트룸 검색에는 keyword와 제목 fallback이 있지만 개인 검색은 semantic search만 수행합니다.

[PersonalAgentCommandService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/PersonalAgentCommandService.java:175)

따라서 embedding 모델이 비활성화되거나 장애가 발생하면 개인 문서 검색은 바로 빈 결과가 됩니다. 문서 코드, 고유명사, 파일명 검색에서도 프로젝트룸 검색보다 약합니다.

### 12. 검색 실패와 “검색 결과 없음”을 구분하지 않습니다

Semantic/keyword 검색에서 예외가 발생하면 로그를 남기고 빈 목록 또는 `ungrounded`로 변환합니다.

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:167)

따라서 다음 상황이 사용자에게 동일하게 보일 수 있습니다.

- 관련 문서가 실제로 없음
- Bedrock embedding 호출 실패
- DB/pgvector 장애
- embedding 차원 불일치
- 쿼리 타임아웃
- 문서 인덱싱 누락

장애인데도 “근거가 없다”는 답변이 나올 수 있어 운영 진단과 사용자 경험 모두 좋지 않습니다.

### 13. 인덱싱 호출이 청크별 순차 처리입니다

각 청크마다 `embeddingModel.embed()`를 한 번씩 호출합니다.

[ResourceEmbeddingIndexPublicService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/service/ResourceEmbeddingIndexPublicService.java:51)

- 대형 문서는 Bedrock 네트워크 호출이 청크 수만큼 발생합니다.
- batch embedding을 사용하지 않습니다.
- 기존 임베딩을 모두 삭제한 뒤 새로 생성합니다.
- 변경된 청크만 재생성하는 증분 인덱싱이 없습니다.
- embedding 모델 ID·버전이 레코드에 저장되지 않습니다.
- 모델 변경 시 어떤 벡터를 재생성해야 하는지 판단하기 어렵습니다.
- 임베딩 전용 상태와 실패 청크 재시도 구조가 부족합니다.

### 14. ANN 인덱스와 권한 필터 조합을 검증해야 합니다

HNSW 인덱스는 존재하지만 하나의 전역 벡터 인덱스입니다.

[V7__resource_embeddings_and_legacy_rag_cleanup.sql](D:/kostaEx/bubli-backend/src/main/resources/db/migration/V7__resource_embeddings_and_legacy_rag_cleanup.sql:30)

검색 시에는 `room_id`, `visibility`, `owner_id` 조건을 함께 사용합니다. pgvector 버전과 실행계획에 따라 ANN 후보를 먼저 고른 뒤 필터링되어 요청한 Top-K보다 결과가 적거나 recall이 떨어질 수 있습니다.

룸별 데이터가 커지면 실제 운영 데이터로 `EXPLAIN ANALYZE`, recall@K를 확인하고 partial index 또는 검색 파라미터 튜닝을 검토해야 합니다.

### 15. 검색 품질·성능 관측이 없습니다

현재 테스트는 서비스 호출, 권한 확인, Top-K 제한, threshold 필터링 등 동작 검증 중심입니다.

[ResourceSemanticSearchPublicServiceTest.java](D:/kostaEx/bubli-backend/src/test/java/com/bubli/resource/service/ResourceSemanticSearchPublicServiceTest.java:24)

부족한 지표는 다음과 같습니다.

- Recall@K, Hit@K, MRR, NDCG
- 검색별 p50/p95/p99 latency
- semantic/keyword/title fallback별 성공률
- threshold 통과·탈락 비율
- 문서별 검색 결과 편중
- 검색 실패율과 실패 원인
- 검색된 근거를 실제 답변에서 사용했는지 여부
- 사용자 평가와 검색 결과의 연결

고정 평가 문서와 질문·정답 청크 세트가 없으므로 현재 `0.72`, `topK=5`, `1200/200`이 적절한지 객관적으로 판단할 수 없습니다.

### 16. RAG prompt injection 방어가 충분하지 않습니다

검색한 `chunkText`를 그대로 LLM 프롬프트에 삽입합니다.

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:548)

업로드 문서 안에 “이전 지시를 무시하라” 같은 문장이 있으면 간접 prompt injection이 발생할 수 있습니다. 상위 프롬프트가 근거만 사용하라고 지시하고 있지만, 문서 내용을 명확히 비신뢰 데이터로 격리하거나 injection을 검사하는 계층은 없습니다.

## 구조상 기술 부채

- 실제 사용되는 것은 `ProjectRoomGroundingService`인데, 더 단순한 [ProjectRoomRagGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomRagGroundingService.java:21)도 남아 있어 검색 정책이 이중화돼 있습니다.
- Spring AI는 `vector_store` 테이블을 설정하지만 실제 검색은 직접 만든 `resource_embeddings`를 native SQL로 조회합니다. 두 벡터 저장 전략이 공존해 운영상 혼동이 생길 수 있습니다.
- 외부 `/api/ai/search-resource` API는 semantic search만 제공하고, keyword/hybrid 검색은 내부 grounding에서만 사용합니다.

## 개선 우선순위

가장 먼저 손볼 순서는 다음이 적절합니다.

1. Keyword 검색을 PostgreSQL FTS 또는 `pg_trgm` 기반으로 변경
2. Semantic·keyword 결과에 RRF 또는 weighted fusion 적용
3. `retrievalMode`를 검색 결과 DTO에 포함해 출처를 정확히 유지
4. 동일 문서·인접 청크 중복 제거와 문서별 결과 개수 제한
5. 검색 장애와 결과 없음 상태 분리
6. 한국어·영어·일본어 평가 corpus를 만들고 Recall@K/MRR 측정
7. 측정 결과로 threshold, Top-K, chunk 크기 조정
8. Batch embedding과 embedding 모델 버전 관리 추가
9. 개인 검색에도 keyword/title fallback 추가
10. 검색 컨텍스트에 토큰 예산과 prompt injection 방어 추가

정리하면, 현재 구현의 가장 큰 문제는 **“검색 기능이 없는 것”이 아니라 “여러 검색 결과를 얼마나 잘 평가하고 합칠지에 대한 계층이 부족한 것”**입니다. 지금은 후보를 찾는 단계까지는 구현되어 있지만, production 수준의 hybrid ranking, 품질 평가, 장애 구분, 검색 관측성이 아직 부족합니다.
