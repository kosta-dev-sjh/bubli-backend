# RAG Agent Postman E2E Guide

This guide covers the manual end-to-end checks for the RAG agent flow through R13.

## Runtime modes

Use the `local` profile for deterministic local execution:

```bash
SPRING_PROFILES_ACTIVE=local
AGENT_EXECUTION_MODE=local
AGENT_DISPATCH_ADAPTER=in-memory
AGENT_WORKER_SCHEDULER_ENABLED=true
```

Use the `ai` profile when the LLM agent and Redis queue are available:

```bash
SPRING_PROFILES_ACTIVE=ai
AGENT_EXECUTION_MODE=llm
AGENT_DISPATCH_ADAPTER=redis
AGENT_WORKER_SCHEDULER_ENABLED=true
AGENT_REDIS_QUEUE_KEY=bubli:agent-jobs
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=replace-with-key
AWS_SECRET_ACCESS_KEY=replace-with-secret
AWS_SESSION_TOKEN=replace-when-using-temporary-credentials
BEDROCK_CHAT_MODEL_ID=apac.anthropic.claude-3-haiku-20240307-v1:0
BEDROCK_EMBEDDING_MODEL_ID=amazon.titan-embed-text-v2:0
```

For local E2E with the local datasource and real Bedrock/Redis execution, run both profiles:

```bash
SPRING_PROFILES_ACTIVE=local,ai
```

## Collection variables

Set these variables before running `docs/http/agent.http` or importing equivalent requests into Postman:

- `baseUrl`: local API base URL, usually `http://localhost:8080`
- `accessToken`: bearer token for a user with access to the project room
- `roomId`: project room ID used for room-scoped jobs
- `resourceId`: uploaded or seeded resource ID
- `relatedResourceId`: another resource ID for document draft source coverage
- `summaryDate`: summary target date, for example `2026-07-01`

## Happy path

1. Create or upload at least two resources in the same project room.
2. Run `POST /api/ai/analyze-resource` for each resource.
3. Poll `GET /api/agent-jobs/{jobId}` until the job is `COMPLETED`.
4. Confirm `GET /api/resources/{resourceId}/ai-document` returns generated analysis.
5. Confirm `GET /api/resources/{resourceId}/related` returns relation candidates after indexing.
6. Run `POST /api/ai/search-resource` for both `PERSONAL` and `ROOM_SHARED` scopes.
7. Run room jobs: requirements, tasks, WBS, questions, and contract review.
8. Run `POST /api/ai/summarize-day` with `summaryDate`.
9. Run `POST /api/ai/draft-document` with `documentType`, `sourceResourceIds`, and `instruction`.
10. List suggestions with `GET /api/agent/suggestions` or `GET /api/project-rooms/{roomId}/agent/suggestions`.
11. Optionally edit a suggestion with `PATCH /api/agent/suggestions/{suggestionId}` and action `EDIT`.
12. Approve a suggestion with action `APPROVE`.
13. Verify the applied domain records through daily summaries, tasks, WBS items, schedules, and AI documents.

## Worker and queue checks

For local mode, the in-memory dispatch adapter should enqueue after transaction commit and the worker scheduler should consume the queue automatically.

For Redis mode, confirm Redis contains messages under `AGENT_REDIS_QUEUE_KEY` while jobs are pending and that the queue drains as the worker processes jobs.

If a job stays `PENDING`, check:

- `agent.dispatch.outbox.scheduler.enabled`
- `agent.dispatch.worker.scheduler.enabled`
- `agent.dispatch.adapter`
- Redis connectivity when using the `redis` adapter

## Failure checks

Run these negative checks during manual QA:

- Search with `ROOM_SHARED` and no `roomId`; expect validation or access failure.
- Request a resource from a room the user cannot access; expect forbidden access.
- Submit an invalid `documentType`; expect validation or a failed job event.
- Approve a suggestion twice; expect the second approval to be rejected or have no duplicate domain side effect.
- Disable the worker scheduler, create a job, then re-enable it and confirm queued work resumes.

## AI quality checks

Use these checks after a real Bedrock E2E run:

- Job responses must be valid `analysis.v1` JSON internally. If Bedrock returns prose or markdown, the JSON repair retry should recover once; otherwise the job should fail with `AI_INVALID_OUTPUT`.
- Every generated suggestion should have a concrete Korean `title`, `description`, and `sourceText`.
- `ANALYZE_RESOURCE` suggestions should quote or paraphrase evidence from the uploaded TXT/PDF text.
- `CONTRACT_FIELD` suggestions must include `fieldKey` and `value`.
- `DAILY_SUMMARY` suggestions should include `done`, `remaining`, `tomorrowFocus`, `risks`, and `evidence` in the generated draft.
- Re-uploading or re-analyzing the same checksum in the same visibility scope should avoid unnecessary Bedrock analysis calls when a reusable LLM analysis summary already exists.
- When usage limits are enabled with `AGENT_MODEL_USER_DAILY_LIMIT` or `AGENT_MODEL_JOB_TYPE_DAILY_LIMIT`, exceeded jobs should fail before calling Bedrock.
- Completion and failure should create notifications visible from `GET /api/notifications`; websocket subscribers to `/user/queue/notifications` should receive the same notification after commit.

The fixture regression suite for these schema expectations is:

```bash
./gradlew test --tests com.bubli.agent.model.AgentAnalysisFixtureRegressionTest
```

## Verified Result

2026-06-30 local E2E verification passed with `local,ai` profiles:

- AWS Bedrock smoke: chat call succeeded, Titan embedding returned 1024 dimensions.
- Redis queue: `bubli:agent-jobs` received jobs and drained to `0` after worker processing.
- TXT and PDF resources uploaded as `ROOM_SHARED`.
- `ANALYZE_RESOURCE` succeeded for TXT and PDF resources.
- `ROOM_SHARED` semantic search returned both TXT and PDF hits with page metadata.
- `GET /api/resources/{resourceId}/related` returned a relation candidate.
- `GENERATE_TASKS` created an LLM suggestion.
- `APPROVE` on the task suggestion created a room task.
