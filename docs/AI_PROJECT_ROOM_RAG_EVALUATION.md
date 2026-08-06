# Project Room RAG Evaluation

This evaluation path measures the project-room side-chat grounding flow, not the semantic-search-only public search API.

## What it measures

- Final document evidence Hit@K, Recall@K, MRR@K, NDCG@K
- Context Precision@K
- Grounded / no-answer decision accuracy
- Grounded precision, recall, F1, no-answer recall, balanced accuracy
- Retrieval failure rate
- Average selected document evidence count
- Selected retrieval mode counts
- Relevant-hit contribution by retrieval mode
- Expected retrieval mode accuracy when `expectedRetrievalModes` is present
- Locale and intent breakdowns
- Average, p50, p95, p99 latency

The runner calls:

```text
POST /api/ai/evaluate-project-room-rag
```

The endpoint executes `ProjectRoomGroundingService.retrieve(...)` and returns the grounding context without generating an LLM answer or persisting chat messages.

## Dataset shape

The runner accepts the existing search dataset shape:

```json
{
  "id": "md-requirement-id",
  "roomId": "...",
  "query": "REQ-MD-003",
  "relevant": [
    {
      "resourceId": "...",
      "chunkIndex": 12
    }
  ]
}
```

For RAG-specific cases, prefer:

```json
{
  "id": "md-requirement-id-ko",
  "roomId": "...",
  "message": "REQ-MD-003 내용을 알려줘",
  "locale": "ko-KR",
  "mode": "ANSWER",
  "intent": "REQUIREMENT_LOOKUP",
  "expectedGrounded": true,
  "relevant": [
    {
      "resourceId": "...",
      "chunkIndex": 12
    }
  ],
  "expectedRetrievalModes": ["KEYWORD", "TITLE_SCOPED_KEYWORD"]
}
```

If `expectedGrounded` is omitted, the runner treats cases with one or more `relevant` items as grounded cases, and empty `relevant` arrays as no-answer cases.

`intent` is optional, but recommended. The report groups metrics by `locale` and `intent`, so adding stable intent labels makes regressions easier to localize.

The current report schema is `2`. It keeps the original retrieval metrics and adds judgement/calibration metrics:

- `metrics.grounding.precision`: of grounded responses, how many should have been grounded.
- `metrics.grounding.recall`: of answerable questions, how many were grounded.
- `metrics.grounding.noAnswerRecall`: of no-answer questions, how many were correctly rejected.
- `metrics.grounding.balancedAccuracy`: average of grounded recall and no-answer recall.
- `metrics.contextPrecisionAtK`: of selected document evidence, how much is relevant.

## Generate a baseline

Run this on the pre-improvement commit or branch, using the same DB snapshot and dataset that will be used for candidate evaluation.

```powershell
./scripts/rag/evaluate-project-room-rag.ps1 `
  -Dataset ./scripts/rag/bubli-search-baseline-v1-mapped.json `
  -Output ./build/reports/rag/project-room-rag-baseline.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN
```

## Generate a candidate report

Run this on the improved implementation.

```powershell
./scripts/rag/evaluate-project-room-rag.ps1 `
  -Dataset ./scripts/rag/bubli-search-baseline-v1-mapped.json `
  -Output ./build/reports/rag/project-room-rag-candidate.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN
```

## Compare before and after

```powershell
./scripts/rag/compare-project-room-rag-reports.ps1 `
  -Baseline ./build/reports/rag/project-room-rag-baseline.json `
  -Candidate ./build/reports/rag/project-room-rag-candidate.json `
  -Output ./build/reports/rag/project-room-rag-comparison.json
```

Higher quality and judgement metrics are better. Lower error rate, retrieval failure rate, and latency are better.

For a fair comparison, baseline and candidate must use the same dataset hash, same topK, same DB snapshot, same embedding model, and similar cache warm-up conditions.
