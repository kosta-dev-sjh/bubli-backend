# AI/RAG/Embedding/Chatbot 성능 개선 및 검증 보고서

작성 기준: 2026-07-09
대상 범위: `agent`, `resource`, `chat`, `memory` 도메인의 AI Agent, RAG, 임베딩 검색, 프로젝트룸 챗봇 기능

## 1. 개선 개요

Bubli 백엔드는 단순 챗봇 응답이 아니라, 프로젝트룸 문서와 업무 데이터를 근거로 답변과 제안을 생성하는 AI 보조 기능을 목표로 개선되었다. 핵심 개선 방향은 다음과 같다.

- 문서 텍스트를 chunk 단위로 분리하고 pgvector 기반 임베딩 검색을 적용해, 사용자의 질문과 관련된 문서 근거를 빠르게 찾도록 개선했다.
- 프로젝트룸, 개인 자료, 리소스 ID, 키워드 검색 범위를 분리해 불필요한 검색 범위를 줄이고 권한 누락 위험을 낮췄다.
- AI 응답을 곧바로 업무 데이터로 반영하지 않고 `agent_suggestions`에 초안으로 저장한 뒤, 사용자의 승인/수정/보류/거절 흐름을 거치도록 했다.
- LLM 호출 실패, 잘못된 JSON 응답, 사용량 초과, 근거 부족 상황을 명시적으로 처리해 운영 안정성을 높였다.
- AI가 생성한 답변, 제안, 문서 초안, 일일 요약, 작업/WBS/일정 반영 흐름을 테스트로 검증했다.

## 2. 성능 개선 내용

### 2.1 RAG 검색 성능 개선

문서 검색은 `resource_embeddings` 테이블과 pgvector를 기반으로 구현되어 있다. `V7__resource_embeddings_and_legacy_rag_cleanup.sql`에서 `vector` extension을 활성화하고, `embedding vector_cosine_ops`에 HNSW 인덱스를 적용했다.

개선 효과:

- 전체 문서를 매번 LLM 프롬프트에 넣지 않고 관련 chunk만 검색하므로 프롬프트 길이와 LLM 비용을 줄일 수 있다.
- `room_id + visibility`, `owner_id + visibility`, `resource_id` 인덱스를 통해 프로젝트룸/개인 범위 검색의 필터링 비용을 줄였다.
- cosine similarity 기반 정렬로 의미적으로 가까운 chunk를 우선 반환한다.

관련 구현:

- `ResourceEmbeddingRepository.searchRoomShared(...)`
- `ResourceEmbeddingRepository.searchPersonal(...)`
- `ResourceSemanticSearchPublicService.search(...)`
- `ProjectRoomRagGroundingService.retrieve(...)`

### 2.2 Chunking 품질 개선

`TextChunker`는 문서를 최대 1,200자 단위로 분리하고 200자 overlap을 적용한다. 단순 고정 길이 분할이 아니라 문단, 문장, 공백 경계를 우선해 chunk를 끊는다.

개선 효과:

- 검색 결과가 문장 중간에서 잘리는 비율을 줄여 답변 근거 품질을 높였다.
- overlap을 통해 경계부 문맥 손실을 줄였다.
- PDF page number, line number, offset metadata를 함께 저장해 답변 근거 추적성을 높였다.

### 2.3 검색 범위 제한 및 Top-K 제어

`ResourceSemanticSearchPublicService`는 기본 `topK=5`, 최대 `topK=20`으로 검색 결과 수를 제한한다. 또한 room shared, personal, resourceIds, keyword 기반 검색을 분리했다.

개선 효과:

- 불필요한 chunk 조회와 프롬프트 주입을 줄였다.
- 특정 리소스 질문은 해당 리소스 안에서만 검색할 수 있어 정확도와 속도를 함께 개선했다.
- embedding model이 비활성화된 환경에서는 명확히 실패하거나 keyword/대표 chunk 경로를 사용할 수 있어 로컬 개발 안정성이 좋아졌다.

### 2.4 LLM 호출 안정성 개선

LLM 실행 포트는 JSON schema 기반 응답을 요구하고, 최초 응답이 계약을 만족하지 않으면 JSON repair prompt로 한 번 더 보정한다. 이후 `AgentAnalysisResultJsonParser`와 validator로 schema version, 필수 필드, suggestion type을 검증한다.

개선 효과:

- LLM 응답 형식 오류로 인한 후속 DB 저장 실패를 줄였다.
- 잘못된 모델 출력은 `AI_INVALID_OUTPUT`으로 분류되어 job 실패 원인을 추적할 수 있다.
- provider 장애는 `AI_PROVIDER_UNAVAILABLE`로 구분되어 운영 대응이 쉬워졌다.

### 2.5 중복 LLM 호출 절감

`LlmAgentJobExecutionPort`는 `ANALYZE_RESOURCE` 처리 시 한국어 응답 조건에서 기존 분석 결과를 재사용할 수 있는 경로를 제공한다.

개선 효과:

- 동일 리소스 재분석 시 불필요한 LLM 호출을 줄일 수 있다.
- 토큰 비용과 응답 대기 시간을 함께 줄이는 구조다.

### 2.6 비동기 Agent Job 처리

AI 기능은 요청 즉시 무거운 작업을 완료하는 방식이 아니라 `agent_jobs`를 생성하고 dispatch/worker가 처리하는 구조다.

개선 효과:

- 긴 LLM 호출이나 문서 분석이 API 응답 시간을 직접 막지 않는다.
- job status, event, model call log를 통해 진행 상태와 실패 원인을 추적할 수 있다.
- `local`, `llm`, `noop` 실행 모드를 분리해 로컬 개발, 실제 AI 실행, API 계약 검증을 각각 독립적으로 수행할 수 있다.

### 2.7 권한 및 데이터 격리 개선

RAG 검색 전 `ProjectRoomAccessPublicService.requireRoomMember(...)`를 호출해 프로젝트룸 멤버 여부를 확인한다. 개인 검색은 `owner_id`, 프로젝트룸 검색은 `room_id`와 `visibility`를 기준으로 제한한다.

개선 효과:

- AI 검색 결과에 권한 없는 문서가 섞이는 위험을 줄였다.
- 검색 성능 개선과 보안 격리를 같은 조건절에서 함께 달성했다.

## 3. 테스트 결과

아래 결과는 2026-07-09 로컬 환경에서 Gradle 테스트를 직접 실행해 확인했다.

실행 명령:

```powershell
.\gradlew.bat test --console=plain
```

전체 테스트 결과:

| 항목 | 결과 |
|---|---:|
| 테스트 리포트 파일 수 | 132 |
| 전체 테스트 수 | 732 |
| 실패 | 0 |
| 에러 | 0 |
| 스킵 | 111 |
| 빌드 결과 | SUCCESS |
| 소요 시간 | 약 34초 |

AI/RAG/챗봇 관련 테스트 결과:

| 영역 | 테스트 클래스 수 | 테스트 수 | 실패 | 에러 | 스킵 |
|---|---:|---:|---:|---:|---:|
| Agent | 42 | 232 | 0 | 0 | 2 |
| Resource/RAG | 17 | 103 | 0 | 0 | 8 |
| Chat | 4 | 25 | 0 | 0 | 5 |
| Memory | 1 | 4 | 0 | 0 | 0 |
| 합계 | 64 | 364 | 0 | 0 | 15 |

추가로 AI/RAG/챗/메모리 영역만 별도 실행했을 때도 빌드가 성공했다.

```powershell
.\gradlew.bat test --tests com.bubli.agent.* --tests com.bubli.resource.service.* --tests com.bubli.chat.* --tests com.bubli.memory.* --console=plain
```

결과: `BUILD SUCCESSFUL`, 소요 시간 약 15초.

## 4. 테스트 커버리지

현재 Gradle 설정에는 JaCoCo 같은 라인 커버리지 측정 플러그인이 연결되어 있지 않다. 따라서 이번 보고서에서는 코드 라인 기준 커버리지 퍼센트가 아니라 기능/위험 기반 커버리지로 정리한다.

검증된 주요 범위:

- Agent job 생성, dispatch, retry, worker, execution result recorder
- LLM execution port, local/noop execution port
- AI 모델 응답 JSON parser, fixture regression, schema validation
- Agent suggestion 조회, 수정, 승인, 도메인 반영
- 문서 업로드, 파일 검사, 텍스트 추출, chunking
- embedding vector formatting, embedding indexing
- semantic search 권한/범위/topK 처리
- resource relation 자동 생성
- 프로젝트룸 agent command, RAG grounding, fallback answer
- 일반 채팅방 생성, 메시지 전송, 읽음 처리, typing event
- daily summary draft 생성/수정
- architecture/entity/flyway alignment 회귀 검증

커버리지 보완 필요:

- 실제 AWS Bedrock/Titan 모델을 호출하는 운영형 E2E는 환경 변수와 외부 계정이 필요하므로 자동 테스트에서는 제한적으로 검증된다.
- 성능 수치의 before/after 비교를 위해서는 고정 문서 corpus, 동시 요청 수, p95 latency, token usage, 검색 hit quality 지표를 별도로 수집해야 한다.
- JaCoCo를 추가하면 라인/브랜치 커버리지 수치를 CI 산출물로 제공할 수 있다.

## 5. AI 적용 효과

### 5.1 문서 기반 답변 정확도 개선

챗봇은 프로젝트룸 문서, TODO, WBS, 일정, 기존 AI 제안 등을 grounding source로 사용한다. RAG 결과가 없으면 일반 지식으로 추정하지 않고 `NO_GROUNDING`, `NO_CHAT_MODEL`, `LLM_FAILED` 등으로 fallback 이유를 남긴다.

효과:

- 답변이 프로젝트 자료에 근거하도록 제한되어 환각 가능성을 줄인다.
- 응답 metadata에 `citations`, `ragHits`, `similarityScore`, `pageNumber`, `lineNumber`가 포함되어 사용자가 근거를 확인할 수 있다.
- 자료 목록 요청과 자료 내용 요약 요청을 분리해 모호한 질문에서 잘못된 답변을 줄인다.

### 5.2 업무 자동화 효과

AI 결과는 다음 형태의 업무 초안으로 연결된다.

- 요구사항 후보 생성
- TODO/Task 후보 생성
- WBS 후보 생성
- 확인 질문 생성
- 계약/문서 검토 항목 생성
- 문서 초안 생성
- 일일 요약 draft 생성

사용자가 승인한 suggestion만 실제 업무 도메인에 반영되므로, AI 자동화의 속도와 사용자 검토 안전장치를 함께 확보했다.

### 5.3 운영 안정성 효과

- model call log로 prompt version, schema version, latency, token 추정치, error code를 기록한다.
- user/job type 단위 일일 사용량 제한을 둘 수 있어 비용 폭주를 방지할 수 있다.
- LLM provider 장애와 JSON 계약 위반을 구분해 장애 원인 분석이 쉬워졌다.

## 6. 성능 개선 요약

| 개선 항목 | 기존 한계 | 개선 내용 | 기대 효과 |
|---|---|---|---|
| 문서 검색 | 전체 문서 프롬프트 주입 시 비용 증가 | chunk + pgvector + HNSW | 검색 속도 개선, 프롬프트 축소 |
| 문맥 품질 | 고정 길이 분할 시 문장 절단 | 문단/문장/공백 경계 chunking | 답변 근거 품질 개선 |
| 검색 범위 | 불필요한 전체 검색 가능성 | room/personal/resourceIds/keyword 분리 | 정확도 및 보안 개선 |
| 응답 안정성 | LLM JSON 오류 가능 | schema validation + repair prompt | 후속 처리 실패 감소 |
| 비용 제어 | 반복 분석과 과도한 호출 위험 | 분석 재사용, 사용량 guard | 비용 및 latency 감소 |
| API 응답성 | 긴 AI 작업이 요청을 막을 수 있음 | async job/worker 구조 | 사용자 체감 대기 감소 |
| 업무 반영 | AI 결과 직접 반영 위험 | suggestion review workflow | 안전한 human-in-the-loop |

## 7. 한계 및 다음 개선 과제

- JaCoCo를 도입해 라인/브랜치 커버리지 수치를 CI에서 자동 산출한다.
- RAG 성능 벤치마크용 고정 corpus를 만들고, 검색 latency, p95/p99, hit@K, MRR, token usage를 측정한다.
- 실제 Bedrock/Titan 연동 smoke test를 별도 profile과 secret 환경에서 주기적으로 실행한다.
- 한국어/영어/일본어 locale 응답 품질 평가 세트를 추가한다.
- RAG 답변에 대한 사용자 평가 데이터를 수집해 similarity threshold와 topK를 조정한다.
