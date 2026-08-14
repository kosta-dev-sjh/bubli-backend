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
- Run metadata: application commit, branch, document snapshot, chunking version, embedding model version, search config
- Warm-up and repeated sequential runs
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

The current report schema is `3`. It keeps the original retrieval metrics and adds judgement/calibration metrics:

- `metrics.grounding.precision`: of grounded responses, how many should have been grounded.
- `metrics.grounding.recall`: of answerable questions, how many were grounded.
- `metrics.grounding.noAnswerRecall`: of no-answer questions, how many were correctly rejected.
- `metrics.grounding.balancedAccuracy`: average of grounded recall and no-answer recall.
- `metrics.contextPrecisionAtK`: of selected document evidence, how much is relevant.
- `runMetadata`: commit/config/snapshot information needed to reproduce the run.
- `metrics.answerQuality`: currently marked as not evaluated because this endpoint does not generate final LLM answers.

## Generate a baseline

Run this on the pre-improvement commit or branch, using the same DB snapshot and dataset that will be used for candidate evaluation.

```powershell
./scripts/rag/evaluate-project-room-rag.ps1 `
  -Dataset ./scripts/rag/bubli-search-baseline-v1-mapped.json `
  -Output ./build/reports/rag/project-room-rag-baseline.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN `
  -WarmupCount 5 `
  -RepeatCount 3 `
  -RunLabel baseline-v1 `
  -DocumentSnapshot local-db-2026-08-06 `
  -ChunkingVersion resource-embedding-v1 `
  -EmbeddingModelVersion configured-runtime `
  -SearchConfig weighted-fusion
```

## Generate a candidate report

Run this on the improved implementation.

```powershell
./scripts/rag/evaluate-project-room-rag.ps1 `
  -Dataset ./scripts/rag/bubli-search-baseline-v1-mapped.json `
  -Output ./build/reports/rag/project-room-rag-candidate.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN `
  -WarmupCount 5 `
  -RepeatCount 3 `
  -RunLabel candidate-answerability-rrf `
  -SearchConfig weighted-plus-rrf-answerability
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

## Compare multiple ablations

```powershell
./scripts/rag/compare-project-room-rag-ablation.ps1 `
  -Baseline ./build/reports/rag/project-room-rag-baseline.json `
  -Candidates @(
    ./build/reports/rag/project-room-rag-candidate-answerability.json,
    ./build/reports/rag/project-room-rag-candidate-rrf.json
  ) `
  -Output ./build/reports/rag/project-room-rag-ablation.json
```

## Failure injection

This verifies that evaluator-level failures are counted separately from no-answer decisions.

```powershell
./scripts/rag/test-project-room-rag-evaluation-failure.ps1 `
  -Dataset ./scripts/rag/bubli-project-room-rag-baseline-v1.json `
  -Output ./build/reports/rag/project-room-rag-failure-injection.json
```

Final answer correctness, faithfulness, citation precision/recall, and locale-match evaluation still require an answer-generating evaluation endpoint or an offline answer report. Until then, this runner intentionally reports `evaluationScope=GROUNDING_ONLY`.

## Candidate and gate diagnostics

The evaluation response and runner now preserve `retrievalDiagnostics` for each successful case. This separates an empty retrieval from a candidate that was intentionally rejected by the answerability gate.

`retrievalDiagnostics` contains the normalized query, locale, extracted/ranking keywords, candidate counts, representative-fallback eligibility, and an `initialFusion`/`finalFusion` summary. Each fusion summary includes the gate reason and score, candidates selected before the gate, final selected candidates, and the top ranked candidate metadata (resource/chunk, retrieval mode, semantic score, fusion score, RRF score, keyword matches, and match reason).

For a case-level evidence archive, add `-IncludeEvidenceDetails`:

```powershell
./scripts/rag/evaluate-project-room-rag.ps1 `
  -Dataset ./scripts/rag/bubli-project-room-rag-baseline-v1.json `
  -Output ./build/reports/rag/project-room-rag-diagnostics.json `
  -ApiBaseUrl http://localhost:8080 `
  -BearerToken $env:BUBLI_EVALUATION_TOKEN `
  -IncludeEvidenceDetails
```

The report also aggregates `metrics.answerabilityReasonCounts`. Do not use the detailed-evidence switch for routine benchmark artifacts if report size or document excerpts are a concern.

## Same-language document search policy

Each indexed document stores a detected `documentLanguage` (`ko`, `en`, `ja`, or `unknown`) in every chunk's metadata. Project-room RAG detects the question language and keeps only chunks with the same language across semantic, keyword, title-scoped, and representative retrieval. It does not translate a query or use cross-lingual retrieval.

Requirement identifiers (`REQ-...`) and explicit resource filenames remain language-independent lookup exceptions. Existing chunks are backfilled by migration `V35`; re-indexing a document recalculates language from the complete extracted text and is more reliable for mixed-script content.
