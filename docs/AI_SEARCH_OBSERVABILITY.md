# AI 검색 품질·성능 관측

이 기능은 검색 알고리즘 변경 전후를 같은 기준으로 비교하기 위한 baseline 도구다.
운영 메트릭과 오프라인 품질 평가를 분리한다.

## 1. 운영 메트릭

`/actuator/prometheus`에서 다음 메트릭을 제공한다.

| Metric | 설명 | 주요 label |
|---|---|---|
| `bubli_ai_search_requests_total` | 검색 요청, 빈 결과, 오류 수 | `strategy`, `scope`, `outcome`, `error_type` |
| `bubli_ai_search_duration_seconds` | embedding 호출과 DB 조회를 포함한 검색 지연시간 | `strategy`, `scope`, `outcome` |
| `bubli_ai_search_results` | 요청별 반환 hit 수 | `strategy`, `scope` |
| `bubli_ai_search_max_score` | 요청별 최대 검색 점수 | `strategy`, `scope` |
| `bubli_ai_search_candidates_total` | grounding threshold 적용 전 후보 수 | `strategy`, `scope` |
| `bubli_ai_search_accepted_total` | grounding에 채택된 후보 수 | `strategy`, `scope` |
| `bubli_ai_search_rejected_total` | threshold에서 제외된 후보 수 | `strategy`, `scope` |
| `bubli_ai_search_fallback_total` | 대표 청크 fallback 성공·빈 결과 수 | `strategy`, `scope`, `outcome` |

사용자 ID, room ID, query는 label로 넣지 않는다. Prometheus cardinality 증가와 질의 내용 노출을
방지하기 위해서다.

Grafana의 `Bubli/AI Search Observability` 대시보드에서 요청량, 빈 결과율, 오류율,
p50/p95/p99 지연시간, 평균 결과 수와 최대 점수를 확인할 수 있다.

## 2. 품질 평가 데이터셋

`scripts/rag/search-quality-dataset.example.json`을 복사하고 실제 테스트 room/resource ID와
정답 chunk를 입력한다. 최소 30개 이상의 질문을 권장하며 한국어, 영어, 일본어와 다음 유형을
고르게 포함한다.

- 정확한 문서명 또는 요구사항 ID 검색
- 자연어 의미 검색
- 여러 문서가 비슷한 내용을 가진 질문
- 정답이 없는 질문
- 짧은 질의와 후속 질문 형태

`relevant.chunkIndex`를 생략하면 해당 resource의 모든 chunk를 정답으로 인정한다.

## 3. Baseline 생성

AI 프로필이 활성화된 동일 환경과 고정 데이터로 실행한다.

```powershell
./scripts/rag/evaluate-search.ps1 `
  -Dataset ./scripts/rag/search-quality-dataset.json `
  -Output ./build/reports/rag/baseline.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN
```

보고서에는 Hit@K, Recall@K, MRR@K, NDCG@K, 오류율과 latency 평균/p50/p95/p99가 저장된다.
데이터셋 SHA-256도 함께 저장되어 이름만 같고 내용이 다른 데이터셋을 비교하는 실수를 방지한다.

## 4. 변경 후 비교

같은 DB snapshot, 같은 데이터셋, 같은 topK, 같은 embedding 모델로 candidate를 생성한다.

```powershell
./scripts/rag/evaluate-search.ps1 `
  -Dataset ./scripts/rag/search-quality-dataset.json `
  -Output ./build/reports/rag/candidate.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN

./scripts/rag/compare-search-reports.ps1 `
  -Baseline ./build/reports/rag/baseline.json `
  -Candidate ./build/reports/rag/candidate.json `
  -Output ./build/reports/rag/comparison.json
```

품질 지표는 높을수록 좋고 오류율과 latency는 낮을수록 좋다. 캐시 warm-up 요청은 baseline과
candidate에 동일하게 적용하고, 각 실험을 여러 번 실행해 중앙값을 비교한다.

현재 외부 검색 API는 semantic search만 제공하므로 이 평가 도구의 최초 baseline도 semantic
검색 기준이다. 내부 keyword/title 검색까지 같은 데이터셋으로 평가하려면 검색 전략을 명시하는
테스트 전용 runner를 후속으로 추가한다.
