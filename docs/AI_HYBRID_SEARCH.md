현재 구현은 MVP 수준에서는 꽤 탄탄합니다. 권한 범위 분리, pgvector/HNSW, 문서 청크, 제목 기반 보완 검색, 인용 메타데이터까지 있습니다. 이후 1차 고도화로 semantic/keyword/title/representative 후보에 weighted fusion 계층을 추가했고, 2차 고도화로 keyword 검색에 PostgreSQL FTS와 pg_trgm 인덱스를 추가했습니다. 인접·유사 청크 제거와 MMR 형태의 다양성 제어, 검색 장애와 결과 없음의 분리, 프로젝트룸 RAG 전용 평가 경로도 구현됐습니다.

현재 가장 큰 품질 문제는 후보 검색 자체보다 **근거가 질문에 답할 만큼 충분한지 판단하는 answerability 계층**입니다. 2026-08-06 기준선에서 검색 품질은 Hit@5 0.7727이지만 no-answer 정확도는 0이었습니다. 따라서 BM25, chunking, batch embedding을 바로 진행하기 전에 오답을 근거 있음으로 판정하는 false positive를 먼저 줄여야 합니다.

## 현재 검색 구조

프로젝트룸 문서 검색은 대략 다음 순서입니다.

1. 사용자 질문에서 문서·작업·일정 등의 검색 의도 추출
2. semantic search 실행
3. keyword search 실행
4. 최근 문서 제목 최대 30개에서 제목 매칭
5. 제목에 매칭된 문서만 대상으로 semantic/keyword search 재실행
6. semantic/keyword/title/representative 후보를 weighted fusion으로 재랭킹
7. 문서별 결과 편중을 제한한 뒤 최종 청크를 LLM 프롬프트에 추가
8. 문서명·페이지·줄·offset·유사도·fusion score·매칭 이유를 citation/evidence metadata로 반환

실제 메인 경로는 [ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:63)입니다.

## 현재 RAG 전용 기준선

프로젝트룸 사이드채팅 경로를 직접 호출하는 전용 평가 API와 스크립트를 사용해 현재 구현을 측정했습니다.

- Dataset: `scripts/rag/bubli-project-room-rag-baseline-v1.json`
- Report: `build/reports/rag/project-room-rag-baseline-v1.json`
- Dataset SHA-256: `3c131228e17d85a420084937bae4e975c693c8fb897426dbb632fc5fe5351966`
- Top-K: 5
- 전체 49건: grounded 44건, no-answer 5건
- 언어 분포: 한국어 35건, 영어 7건, 일본어 7건

| 지표 | 기준선 |
| --- | ---: |
| Hit@5 | 0.772727 |
| Recall@5 | 0.737500 |
| MRR@5 | 0.703409 |
| NDCG@5 | 0.911414 |
| Grounded accuracy | 0.857143 |
| No-answer accuracy | 0.000000 |
| Retrieval failure rate | 0.000000 |
| 평균 문서 근거 수 | 3.000000 |
| Latency p50 | 173.121 ms |
| Latency p95 | 316.120 ms |
| Latency p99 | 351.471 ms |

이 수치의 핵심 해석은 다음과 같습니다.

- 관련 문서를 찾는 능력은 이미 일정 수준에 도달했지만 아직 약 22.7%의 질문은 Top-5에 정답 청크가 없습니다.
- 5개의 no-answer 질문을 모두 grounded로 판정했습니다. 현재 `groundedAccuracy`가 높아 보이는 이유는 positive 44건에 비해 negative가 5건뿐인 클래스 불균형 때문입니다.
- `KEYWORD`, `TITLE_SCOPED_KEYWORD`, `RECENT_SUMMARY`가 약한 관련성만으로도 answerable 판정을 만드는 경향이 있습니다.
- 이 기준선은 현재 고도화가 일부 적용된 상태의 기준선입니다. 최초 semantic-only 기준선과 이름은 비슷하지만 같은 경로의 성능 비교 자료가 아닙니다.
- 49건을 한 번씩 순차 호출한 latency이므로 부하 성능이나 안정적인 백분위로 해석하면 안 됩니다.

## 주요 한계점

### 1. Hybrid search가 아니라 단순 결과 병합에 가깝습니다

상태: **1차 개선 완료**.

기존에는 semantic 결과와 keyword 결과를 embedding ID 기준으로 합칠 뿐, 두 점수를 통합해서 다시 순위를 계산하지 않았습니다.

- Semantic 점수: cosine similarity
- Keyword 점수: 포함된 키워드 수 ÷ 전체 키워드 수
- 두 점수의 의미와 분포가 전혀 다른데 그대로 섞입니다.
- 제목 매칭이 없으면 semantic 결과 다음에 keyword 결과가 붙는 순서가 됩니다.
- 현재는 `ProjectRoomDocumentFusionService`에서 weighted fusion을 수행합니다.
- RRF feature는 production fusion에 보조 점수로 추가됐고, cross-encoder reranker와 LLM reranker는 아직 없습니다.

관련 병합 코드는 [ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:441)입니다.

따라서 keyword에서 매우 정확하게 일치한 청크가 semantic의 애매한 결과보다 뒤로 밀리는 문제는 상당 부분 개선됐습니다.

### 2. Keyword search의 정확도와 성능이 모두 제한적입니다

상태: **2차 개선 완료**.

기존 keyword 검색은 다음 형태였습니다.

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

현재는 `V34__resource_embedding_keyword_search_indexes.sql`에서 `pg_trgm` extension과 `to_tsvector('simple', chunk_text)` GIN 인덱스를 추가했고, keyword 쿼리는 FTS 매칭, trigram `word_similarity`, 기존 token coverage를 합산합니다.

남은 한계는 다음과 같습니다.

- PostgreSQL `simple` dictionary라 한국어/일본어 형태소 분석은 하지 않습니다.
- BM25가 아니라 FTS rank + token coverage + trigram 보정입니다.
- 키워드는 여전히 최대 5개로 제한됩니다.
- query expansion, synonym expansion, typo correction은 없습니다.

### 3. 검색 결과의 출처 유형이 잘못 기록될 수 있습니다

상태: **개선 완료**.

기존에는 semantic과 keyword 결과를 병합한 뒤 모든 문서 근거에 다음 값을 넣었습니다.

```java
metadata.put("retrievalMode", "SEMANTIC");
```

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:520)

현재는 `SEMANTIC`, `KEYWORD`, `TITLE_SCOPED_SEMANTIC`, `TITLE_SCOPED_KEYWORD`, `REPRESENTATIVE`를 구분해 `retrievalMode`에 기록합니다. 또한 `fusionScore`, `matchedKeywords`, `matchReason`을 evidence metadata에 포함합니다.

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

### 5. 문서 다양성 제어는 구현됐지만 문맥 연결은 부족합니다

상태: **인접·유사 청크 제거와 MMR 형태 선택 완료**.

검색 단위가 청크이므로 같은 문서의 서로 인접한 청크가 Top-K를 전부 차지할 수 있었습니다. 현재 `ProjectRoomDocumentFusionService`는 문서별 최대 2개 제한, 같은 문서의 인접 청크 제외, 5-gram Jaccard 기반 유사 청크 제외, 중복 페널티를 적용한 선택을 수행합니다.

남은 기능은 다음과 같습니다.

- 질문 유형과 문서 수에 따른 동적 문서별 청크 제한
- 정답이 청크 경계에 걸린 경우의 parent/neighbor context 확장
- 같은 문서의 연속 청크를 근거 표시 단계에서 안전하게 병합
- 문서 단위 점수와 청크 단위 점수의 조합
- MMR 상수와 중복 임계값의 평가 corpus 기반 튜닝

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

상태: **기본 개선 완료**.

기존 대표 청크 조회는 다음과 같이 정렬했습니다.

```sql
ORDER BY resource_id ASC, chunk_index ASC
LIMIT :limit
```

[ResourceEmbeddingRepository.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/resource/repository/ResourceEmbeddingRepository.java:204)

현재는 `row_number() over (partition by resource_id order by chunk_index)`를 사용해 각 문서의 첫 번째 청크가 먼저 섞이도록 정렬합니다. 다만 반환 점수는 아직 `1.0`이라 semantic similarity와 구분하기 위해 `retrievalMode=REPRESENTATIVE`와 fusion metadata를 함께 봐야 합니다.

### 11. 개인 자료 검색은 semantic search만 있습니다

프로젝트룸 검색에는 keyword와 제목 fallback이 있지만 개인 검색은 semantic search만 수행합니다.

[PersonalAgentCommandService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/PersonalAgentCommandService.java:175)

따라서 embedding 모델이 비활성화되거나 장애가 발생하면 개인 문서 검색은 바로 빈 결과가 됩니다. 문서 코드, 고유명사, 파일명 검색에서도 프로젝트룸 검색보다 약합니다.

### 12. 검색 실패와 “검색 결과 없음”은 분리됐습니다

상태: **기본 개선 완료**.

Semantic/keyword 검색에서 발생한 예외를 `retrievalFailed`, `retrievalFailureReason`으로 전달하고, 사용자 응답도 locale별 검색 장애 메시지와 `GROUNDING_RETRIEVAL_FAILED` fallback reason으로 분리합니다.

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:167)

현재 구분 가능한 대표 상황은 다음과 같습니다.

- 관련 문서가 실제로 없음
- Bedrock embedding 호출 실패
- DB/pgvector 장애
- embedding 차원 불일치
- 쿼리 타임아웃
- 문서 인덱싱 누락

남은 작업은 실패 원인을 안정적인 코드로 구조화하고, 부분 장애 시 어떤 retrieval 전략까지 성공했는지 기록하며, timeout·DB 장애·embedding 장애를 주입하는 통합 테스트를 추가하는 것입니다.

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

### 15. 검색 품질·성능 관측은 시작됐지만 평가 설계가 충분하지 않습니다

상태: **RAG 전용 기준선 생성 완료, 운영·종단 평가 미완성**.

기존 테스트는 서비스 호출, 권한 확인, Top-K 제한, threshold 필터링 등 동작 검증 중심이었습니다.

[ResourceSemanticSearchPublicServiceTest.java](D:/kostaEx/bubli-backend/src/test/java/com/bubli/resource/service/ResourceSemanticSearchPublicServiceTest.java:24)

부족한 지표는 다음과 같습니다.

- Recall@K, Hit@K, MRR, NDCG
- 검색별 p50/p95/p99 latency: Micrometer timer 추가
- semantic/keyword/title fallback별 성공률: strategy/scope별 metric 일부 추가
- threshold 통과·탈락 비율: candidate/accepted/rejected metric 추가
- 문서별 검색 결과 편중: fusion 단계에서 per-resource cap 추가
- 검색 실패율과 실패 원인: metric 일부 추가
- 검색된 근거를 실제 답변에서 사용했는지 여부
- 사용자 평가와 검색 결과의 연결

현재 `evaluate-project-room-rag.ps1`이 프로젝트룸 grounding 경로의 Hit@K, Recall@K, MRR, NDCG, grounded/no-answer 판정, 검색 실패율, retrieval mode, latency를 측정하고 고정 데이터셋 v1과 기준선 보고서를 생성합니다.

다만 현재 평가에는 다음 한계가 있습니다.

- negative가 5/49에 불과하고 모두 실패했으므로 no-answer 개선 여부를 안정적으로 비교하기 어렵습니다.
- 한국어 35건, 영어 7건, 일본어 7건이라 전체 평균이 한국어 성능에 치우칩니다.
- 같은 의미의 다국어 변형이 많아 질문 유형의 다양성이 작습니다.
- exact chunk index를 정답으로 사용하므로 정답을 충분히 포함한 인접 청크를 오답 처리할 수 있습니다.
- `expectedRetrievalModes`를 데이터셋에 기록하지만 현재 평가 점수에는 반영하지 않습니다.
- grounded boolean만 기록해 threshold calibration, AUROC/AUPRC, Brier score를 계산할 confidence가 없습니다.
- 답변 LLM을 호출하지 않으므로 answer correctness, faithfulness, citation precision, 언어 일치 여부는 측정하지 않습니다.
- warm-up, 반복 실행, 동시성 단계가 없어 latency 비교의 재현성이 낮습니다.

따라서 threshold와 fusion weight를 조정하기 전에 데이터셋과 평가기를 먼저 확장해야 합니다.

### 16. Answerability 판정이 후보 존재 여부에 너무 가깝습니다

상태: **최우선 개선 대상**.

현재 fusion threshold를 통과하거나 requirement ID·따옴표 문구 hard match가 있으면 후보가 선택되고, 최종적으로 선택된 후보가 하나라도 있으면 `grounded=true`가 됩니다. 이 구조는 “질문과 관련된 문서”와 “질문에 답을 제공하는 문서”를 구분하지 못합니다.

기준선의 모든 no-answer 실패가 이 문제를 보여 줍니다. 다음 보강이 필요합니다.

- 문서 검색 의도를 나타내는 일반 단어(`문서`, `자료`, `project`, `documents`, `資料` 등)를 ranking keyword에서 제거
- 전체 키워드 수가 아니라 정보성 키워드의 coverage와 rare-token match를 사용
- `RECENT_SUMMARY` 단독 근거는 개요 질문에만 허용하고, 날짜·정책·수치·조건 질문의 answerability 근거로 사용하지 않음
- title match는 검색 범위 축소 신호로 사용하되, 본문이 질문 핵심을 포함하지 않으면 grounded로 판정하지 않음
- semantic/keyword 점수, rank, query coverage, exact phrase, title match, top-1/top-2 margin을 모은 별도 `answerabilityScore` 도입
- 최소 근거 수를 고정값으로 강제하지 않고, 한 개의 강한 exact evidence 또는 복수 전략의 합의 같은 규칙으로 판정
- grounded 여부와 함께 confidence 및 rejection reason을 평가 응답과 metrics에 기록

초기에는 설명 가능한 규칙 기반 gate로 시작하고, 충분한 labeled corpus가 쌓인 뒤 logistic regression 또는 경량 reranker로 교체하는 편이 안전합니다. 49건으로 학습형 모델을 도입하면 과적합 가능성이 큽니다.

### 17. RAG prompt injection 방어가 충분하지 않습니다

검색한 `chunkText`를 그대로 LLM 프롬프트에 삽입합니다.

[ProjectRoomGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomGroundingService.java:548)

업로드 문서 안에 “이전 지시를 무시하라” 같은 문장이 있으면 간접 prompt injection이 발생할 수 있습니다. 상위 프롬프트가 근거만 사용하라고 지시하고 있지만, 문서 내용을 명확히 비신뢰 데이터로 격리하거나 injection을 검사하는 계층은 없습니다.

## 구조상 기술 부채

- 실제 사용되는 것은 `ProjectRoomGroundingService`인데, 더 단순한 [ProjectRoomRagGroundingService.java](D:/kostaEx/bubli-backend/src/main/java/com/bubli/agent/service/ProjectRoomRagGroundingService.java:21)도 남아 있어 검색 정책이 이중화돼 있습니다.
- Spring AI는 `vector_store` 테이블을 설정하지만 실제 검색은 직접 만든 `resource_embeddings`를 native SQL로 조회합니다. 두 벡터 저장 전략이 공존해 운영상 혼동이 생길 수 있습니다.
- 외부 `/api/ai/search-resource` API는 semantic search만 제공하고, keyword/hybrid 검색은 내부 grounding에서만 사용합니다.

## 개선 우선순위

기존 계획은 검색 후보를 늘리는 데 초점이 강했습니다. 현재 기준선에서는 후보 확장보다 false positive 억제가 먼저이므로 다음 순서로 변경합니다.

### Phase 0. 완료된 기반 작업

1. ~~Keyword 검색을 PostgreSQL FTS와 `pg_trgm` 기반으로 변경~~
2. ~~Semantic·keyword·title·representative 후보에 weighted fusion 적용~~
3. ~~`retrievalMode`, `fusionScore`, `matchedKeywords`, `matchReason` 기록~~
4. ~~문서별 결과 제한, 인접·유사 청크 제거, MMR 형태 다양성 선택~~
5. ~~검색 장애와 결과 없음 상태 분리~~
6. ~~한국어·영어·일본어 RAG 전용 데이터셋과 기준선 생성~~

### Phase 1. 평가 신뢰도와 no-answer 정밀도

1. 데이터셋을 최소 150건으로 확장하고 positive/negative를 약 2:1로 구성
2. 한국어·영어·일본어별 최소 30건을 확보하고 locale macro average 추가
3. 문서에 없는 날짜·금액·정책·인물·버전 질문, 비슷하지만 다른 문서, 부분적으로만 답할 수 있는 질문을 hard negative로 추가
4. 질문 유형을 requirement ID, 제목, exact phrase, 설명형, 요약형, 후속 질문, 오타/표기 변형으로 태깅
5. `answerabilityScore`, confidence, rejection reason을 추가하고 no-answer gate 구현
6. evaluator에 grounded precision/recall/F1, no-answer recall, balanced accuracy, context precision@K, locale·intent별 지표 추가

Phase 1 합격 기준은 다음과 같이 둡니다.

- No-answer accuracy 0.80 이상
- Grounded recall 0.90 이상
- Balanced accuracy 0.85 이상
- Hit@5와 Recall@5는 현재 기준선 대비 0.03 초과 하락 금지
- 한국어·영어·일본어 각각 전체 기준보다 0.10 이상 낮아지지 않음
- 검색 장애를 no-answer로 집계하는 케이스 0건

2026-08-06 1차 구현에서는 `answerabilityScore`/`answerabilityReason`, generic source keyword 제외, `RECENT_SUMMARY`/`TITLE_MATCH`의 개요 질문 제한, evaluator schema v2의 precision/recall/F1/balanced accuracy/context precision/locale·intent breakdown을 추가했습니다. 다음 확인은 동일 데이터셋으로 candidate report를 생성해 no-answer recall과 grounded recall의 trade-off를 비교하는 것입니다.

### Phase 2. 검색 순위 개선

1. 현재 절대 점수 가중 합산과 hard-coded bonus를 RRF 기반 1차 fusion으로 교체하거나 ablation 비교
2. 정보성 token coverage, exact phrase, requirement ID, title, semantic rank를 feature로 한 설명 가능한 2차 reranker 적용
3. 현재 데이터셋에서는 학습형 reranker를 사용하지 않고, dev/test split을 만든 뒤 충분한 사례가 쌓였을 때 도입
4. `RECENT_SUMMARY`와 `REPRESENTATIVE`는 질문 유형에 따라 제한적으로 사용
5. title 검색의 최근 30개 제한과 N+1 조회 제거
6. parent/neighbor chunk 확장과 heading/table-aware chunking 실험

RRF는 점수 분포가 다른 semantic, FTS, trigram 결과를 직접 더하는 현재 방식보다 튜닝 안정성이 좋습니다. 다만 실제 적용 여부는 기존 weighted fusion과 동일 데이터셋에서 ablation으로 결정합니다. cross-encoder/LLM reranker는 품질 이득이 확인될 때만 latency·비용 예산 안에서 추가합니다.

2026-08-06 1차 구현에서는 production fusion에 RRF feature를 보조 점수로 추가하고 `fusionStrategy=WEIGHTED_PLUS_RRF`, `rrfScore`를 evidence metadata에 기록했습니다. 또한 여러 candidate report를 한 baseline과 비교하는 `compare-project-room-rag-ablation.ps1`을 추가했습니다. 실제 RRF 단독 전환은 아직 하지 않고, 동일 dataset hash의 ablation 결과로 결정합니다.

### Phase 3. 종단 답변 품질과 운영성

1. 검색 근거를 사용한 최종 답변의 correctness, faithfulness, citation precision/recall, locale 일치 평가 추가
2. 사람 검수 gold answer를 우선하고 LLM-as-judge는 보조 지표로만 사용
3. warm-up 후 반복 실행과 동시성 1/5/20 부하 시나리오로 p50/p95/p99 재측정
4. dataset hash 외에 문서 snapshot, chunking version, embedding model/version, 검색 설정, application commit을 보고서에 기록
5. retrieval 전략별 ablation과 장애 주입 테스트 추가
6. Batch embedding, 증분 인덱싱, embedding 모델 버전 관리 구현
7. 개인 검색에도 검증된 hybrid 정책 적용
8. 검색 컨텍스트 토큰 예산과 prompt injection 방어 추가

2026-08-06 1차 구현에서는 evaluator schema v3로 run metadata(application commit/branch, document snapshot, chunking version, embedding model version, search config), warm-up, repeat count, request timeout, concurrency label을 기록하도록 확장했습니다. 또한 failure-injection script를 추가해 evaluator/API 실패가 no-answer와 섞이지 않는지 검증할 수 있게 했습니다. 최종 답변 correctness/faithfulness/citation/locale 평가는 현재 endpoint가 grounding-only라 `metrics.answerQuality.evaluated=false`로 명시하고, 별도 answer-generating 평가 endpoint가 필요한 상태로 남겼습니다.

## 평가 데이터 운영 원칙

- 튜닝용 dev set과 최종 판정용 holdout test set을 분리합니다.
- 같은 원문 질문의 단순 번역은 동일 group으로 묶어 dev/test 양쪽에 갈라지지 않게 합니다.
- 정답은 가능하면 resource ID 하나가 아니라 허용 가능한 chunk 범위와 근거 구절을 함께 저장합니다.
- 문서가 바뀌거나 rechunking되면 기존 chunk index 정답을 조용히 재사용하지 않고 dataset version을 올립니다.
- 전체 평균만 보지 않고 locale, intent, 난이도, retrieval mode별 결과를 함께 비교합니다.
- 후보 변경은 한 번에 하나씩 ablation하고, quality gate를 통과한 설정만 새 기준선으로 승격합니다.
- 현재 `project-room-rag-baseline-v1`은 변경하지 않는 기준선으로 보존하고 후속 실행은 candidate 보고서로 생성합니다.

정리하면, 이 문서를 바탕으로 계속 고도화하는 방향은 맞습니다. 다만 다음 개발의 시작점은 BM25나 더 큰 모델이 아니라 **평가 데이터 확장 → answerability/no-answer gate → fusion ablation**이어야 합니다. 현재 병목은 문서를 못 찾는 문제만이 아니라, 찾은 문서가 실제 답을 포함하는지 검증하지 않고 채택하는 문제입니다.
