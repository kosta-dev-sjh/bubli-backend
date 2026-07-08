# 프로젝트 문서 전용 RAG Agent 구현 계획

## Summary

`/bubli` 에이전트 답변의 지식 출처를 프로젝트룸 문서 임베딩 검색 결과로 제한한다. LLM 프롬프트에는 최근 채팅, room memory, task, WBS, schedule 등을 사실 근거로 넣지 않고, `resource_embeddings`에서 검색된 프로젝트 문서 청크만 제공한다. 검색 결과가 없거나 유사도가 기준 미만이면 LLM을 호출하지 않고 `"프로젝트 자료 기준에서는 알 수 없는 내용입니다"` 계열의 locale별 고정 응답을 반환한다.

이 계획에서는 이름 저장, 개인 프로필 기억, 범용 챗메모리 기능은 구현하지 않는다.

## Key Changes

- `ProjectRoomAgentCommandService`의 답변 경로를 RAG-first로 변경한다.
  - 기존 `AgentJobContextCollector` 기반 프로젝트 맥락을 LLM 지식 근거로 사용하지 않는다.
  - `ResourceSemanticSearchPublicService.search(userId, ROOM_SHARED, roomId, message, topK)`를 호출한다.
  - 검색 hit가 없거나 `similarityScore < minSimilarity`이면 LLM 호출 없이 답변 차단 응답을 반환한다.
  - 검색 hit가 있으면 LLM 프롬프트에 `chunkText`, `resourceId`, `chunkIndex`, `pageNumber`, `similarityScore`만 넣는다.

- RAG 전용 컨텍스트 모델을 추가한다.
  - 예: `ProjectRoomRagContext`
  - 포함 필드:
    - `boolean grounded`
    - `List<ResourceSearchHit> hits`
    - `double maxSimilarity`
    - `String promptBlock`
  - `promptBlock`은 검색된 청크만 포함한다.
  - 문서 제목이 필요하면 `ResourcePublicService` 또는 repository 조회로 `resourceId -> title`을 보강하되, 본문 지식은 `chunkText`만 사용한다.

- RAG 검색 정책을 설정값으로 분리한다.
  - 예: `agent.rag.top-k=5`
  - 예: `agent.rag.min-similarity=0.72`
  - 예: `agent.rag.enabled=true`
  - embedding model이 없거나 pgvector 검색이 실패하면 “프로젝트 자료 기준에서는 알 수 없는 내용입니다”로 fail-closed 처리한다.

- LLM 프롬프트를 source-only 방식으로 교체한다.
  - 반드시 검색된 프로젝트 문서 청크만 근거로 답변하도록 지시한다.
  - 프로젝트 문서에 없는 내용은 추측하지 말고 “프로젝트 자료 기준에서는 알 수 없는 내용입니다”라고 답하도록 지시한다.
  - 최근 채팅, room memory, 일반 프로젝트 상태, 사용자 기억을 근거로 쓰지 않도록 명시한다.
  - `SUGGEST` 모드도 검색된 문서 청크가 있을 때만 TODO/TASK/QUESTION/REVIEW_ITEM 후보를 생성한다.

- 응답 body와 memory/evidence에 RAG 근거를 남긴다.
  - `ragGrounded: true/false`
  - `ragMaxSimilarity`
  - `ragHits`: `resourceId`, `chunkIndex`, `pageNumber`, `similarityScore`
  - `resourceIds`: 검색으로 참조된 resource 목록
  - 검색 실패 응답도 chat message로 저장하되, LLM 응답처럼 보이지 않게 `ragGrounded=false`를 남긴다.

## Implementation Details

- 기존 `ResourceSemanticSearchPublicService`와 `resource_embeddings`를 재사용한다.
  - 새 벡터 DB를 만들지 않는다.
  - 기존 `ROOM_SHARED` scope만 사용한다.
  - 개인 자료 `PERSONAL` scope는 `/bubli` 프로젝트룸 agent 답변에 사용하지 않는다.

- `ProjectRoomAgentCommandService.execute(...)` 흐름을 다음 순서로 재정렬한다.
  1. room membership 확인
  2. locale 확인
  3. `ResourceSemanticSearchPublicService`로 room 문서 검색
  4. RAG context가 없으면 locale별 no-answer 반환
  5. RAG context가 있으면 source-only prompt로 LLM 호출
  6. `SUGGEST` 모드면 RAG evidence가 있는 suggestion draft 생성
  7. chat message와 room memory summary 저장

- 기존 `resolveResourceLookup(...)`는 보조 정보 용도로만 유지하거나 RAG 경로에서는 제거한다.
  - “최근 파일 알려줘” 같은 리소스 탐색성 질문은 별도 기능으로 유지 가능하지만, 문서 내용 질의 답변의 근거로는 RAG hits만 사용한다.
  - 구현 시 혼선을 줄이기 위해 RAG 답변 경로와 latest-resource lookup 경로를 메서드 레벨에서 분리한다.

- locale별 no-answer 문구를 고정한다.
  - `ko-KR`: `프로젝트 자료 기준에서는 알 수 없는 내용입니다.`
  - `ja-JP`: `プロジェクト資料の範囲では分かりません。`
  - `en-US`: `I cannot determine that from the project materials.`

## Public Interfaces / Types

- 새 설정 타입을 추가한다.
  - 예: `AgentRagProperties`
  - properties prefix: `agent.rag`
  - fields: `enabled`, `topK`, `minSimilarity`

- 새 내부 서비스 추가를 권장한다.
  - 예: `ProjectRoomRagGroundingService`
  - public method:
    - `ProjectRoomRagContext retrieve(UUID userId, UUID roomId, String query, String locale)`
  - 이 서비스가 검색, threshold 판정, promptBlock 생성을 책임진다.

- 기존 외부 API endpoint는 변경하지 않는다.
  - `/api/project-rooms/{roomId}/agent/commands`
  - request shape 유지: `message`, `mode`, `resourceIds`
  - response body에는 RAG metadata가 추가될 수 있다.

## Test Plan

- `ProjectRoomAgentCommandServiceTest`
  - 검색 hit가 없으면 `ChatModel.call(...)`이 호출되지 않는지 검증
  - no-answer 문구가 locale별로 반환되는지 검증
  - 검색 hit가 threshold 미만이면 LLM 호출 없이 no-answer 반환
  - 검색 hit가 threshold 이상이면 LLM prompt에 `chunkText`가 포함되는지 검증
  - prompt에 최근 채팅, room memory, task, WBS, schedule context가 포함되지 않는지 검증
  - `SUGGEST` 모드에서 RAG hit가 없으면 suggestion draft를 만들지 않는지 검증
  - `SUGGEST` 모드에서 RAG hit가 있으면 evidence에 `ragHits`가 포함되는지 검증

- `ProjectRoomRagGroundingServiceTest`
  - `ROOM_SHARED` scope로만 검색하는지 검증
  - topK와 minSimilarity 설정이 적용되는지 검증
  - embedding model unavailable 예외 발생 시 fail-closed context를 반환하는지 검증
  - hit metadata의 pageNumber/resourceId/chunkIndex가 prompt/evidence에 보존되는지 검증

- 기존 `ResourceSemanticSearchPublicServiceTest`
  - 유지한다.
  - 필요 시 threshold 필터링은 새 RAG grounding service 테스트에서 담당한다.

## Assumptions

- `/bubli` 프로젝트룸 agent의 답변 지식 출처는 `ROOM_SHARED` 프로젝트 문서 임베딩으로 제한한다.
- 최근 채팅과 room memory는 RAG 답변의 사실 근거로 사용하지 않는다.
- 이름 저장, 개인 장기기억, 사용자 프로필 memory 기능은 이번 구현 범위에서 제외한다.
- 검색 결과가 없는 경우에는 LLM에게 판단을 맡기지 않고 서버 코드에서 즉시 no-answer를 반환한다.
- 기본값은 `topK=5`, `minSimilarity=0.72`, `enabled=true`로 둔다.
