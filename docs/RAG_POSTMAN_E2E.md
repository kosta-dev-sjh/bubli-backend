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
OPENAI_API_KEY=replace-with-key
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
