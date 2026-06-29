# RAG Step 0~1 구현 설명

## 1. 현재 구현 범위

`RAG_IMPLEMENTATION_ROADMAP.md`의 다음 범위를 구현했다.

```text
Step 0. 구현 범위와 정책 확정
Step 1. Spring AI와 pgvector 기반 구성
```

이 단계의 목적은 문서 Entity나 업로드 API를 만들기 전에 다음 기반을 먼저 검증하는 것이다.

1. 어떤 문서를 RAG 대상으로 삼을지 결정한다.
2. AI 결과가 지켜야 할 JSON 계약을 정의한다.
3. AWS Bedrock Chat Model을 호출할 수 있게 한다.
4. Titan Embedding으로 임베딩을 생성한다.
5. PostgreSQL pgvector에 벡터를 저장하고 검색한다.
6. AWS 설정이 없어도 일반 백엔드는 정상 실행되게 한다.

---

## 2. Step 0 구현 내용

### 2.1 MVP 정책

정책 문서:

```text
docs/RAG_MVP_POLICY.md
```

현재 결정된 범위:

| 항목 | 결정 |
|---|---|
| 지원 파일 | PDF, TXT |
| 검색 범위 | 프로젝트룸 문서 |
| 개인 문서 | MVP 제외 |
| OCR | MVP 제외 |
| Chat Model | AWS Bedrock Converse |
| Embedding Model | Amazon Titan Text Embeddings V2 |
| Vector Store | PostgreSQL + pgvector |
| 임베딩 차원 | 1024 |
| 벡터 인덱스 | HNSW |
| 거리 계산 | Cosine distance |
| 제안 기본 상태 | PENDING |
| 문서 최대 크기 | 50MB |

에이전트 결과는 실제 업무 데이터를 직접 생성하지 않는다. 항상 후보로 반환하고 사용자 승인 후 다른 도메인 Service가 확정 데이터로 반영한다.

### 2.2 `analysis.v1` JSON 계약

주요 파일:

```text
agent/contract/v1/AgentAnalysisResult.java
agent/contract/v1/Analysis.java
agent/contract/v1/Suggestion.java
agent/contract/v1/SuggestionType.java
agent/validation/AgentAnalysisResultValidator.java
agent/model/AgentAnalysisResultJsonParser.java
resources/schema/agent/analysis-v1.schema.json
```

#### `AgentAnalysisResult`

에이전트의 문서 분석 결과 최상위 구조다.

```text
schemaVersion
resourceId
model
analysis
suggestions
```

`schemaVersion`은 반드시 `analysis.v1`이어야 한다. 앞으로 구조가 변경되면 기존 v1을 수정하는 대신 `analysis.v2`를 추가한다.

#### `Analysis`

문서 자체의 분석 정보를 가진다.

```text
summary
keywords
risks
checklist
```

#### `Suggestion`

에이전트가 만든 후보를 표현한다.

```text
TASK
REQUIREMENT
CONTRACT_FIELD
```

유형별 필수 필드:

| 유형 | 필수값 |
|---|---|
| TASK | `title`, `sourceText` |
| REQUIREMENT | `title`, `description` |
| CONTRACT_FIELD | `fieldKey`, `value` |

`confidence`는 `0.0~1.0` 범위만 허용한다.

#### `AgentAnalysisResultValidator`

Bean Validation으로 기본 필수값과 범위를 검사하고, 제안 유형별 필수 필드를 추가 검사한다.

검증 실패 시 필드 경로와 사유를 `AgentContractError` 목록으로 반환한다.

#### `AgentAnalysisResultJsonParser`

LLM이 반환한 JSON 문자열을 `AgentAnalysisResult`로 변환한다.

다음 경우 파싱을 거부한다.

- 올바르지 않은 JSON
- 알 수 없는 필드
- 지원하지 않는 `schemaVersion`
- 필수 필드 누락
- confidence 범위 위반
- 제안 유형별 필수값 누락

따라서 LLM 응답을 바로 DB에 저장하거나 업무 데이터로 사용하지 않고 반드시 이 파서를 통과시켜야 한다.

---

## 3. Step 1 구현 내용

### 3.1 Spring AI 의존성

파일:

```text
build.gradle
```

Spring AI BOM으로 모듈 버전을 통일한다.

```groovy
implementation platform("org.springframework.ai:spring-ai-bom:${springAiVersion}")
```

추가 모듈:

```groovy
spring-ai-starter-model-bedrock
spring-ai-starter-model-bedrock-converse
spring-ai-starter-vector-store-pgvector
```

역할:

| 모듈 | 역할 |
|---|---|
| Bedrock Converse | LLM Chat 호출 |
| Bedrock Model | Titan Embedding 호출 |
| PGvector Vector Store | 임베딩 저장과 유사도 검색 |

### 3.2 프로필 분리

기본 설정:

```text
src/main/resources/application.yml
```

AI 전용 설정:

```text
src/main/resources/application-ai.yml
```

기본 프로필에서는 다음과 같이 AI 자동 구성을 끈다.

```yaml
spring:
  ai:
    model:
      chat: none
      embedding: none
    vectorstore:
      type: none
```

그 이유는 AWS 키나 모델 접근 권한이 없는 개발자도 일반 API 서버를 실행할 수 있어야 하기 때문이다.

`ai` 프로필을 활성화하면 다음 기능이 켜진다.

```text
Bedrock Converse Chat Model
Titan Embedding Model
PGvector VectorStore
RAG Smoke Service
```

### 3.3 Bedrock 설정

`application-ai.yml`은 민감정보를 직접 저장하지 않고 환경변수를 참조한다.

| 환경변수 | 역할 |
|---|---|
| `AWS_REGION` | Bedrock 사용 리전 |
| `AWS_ACCESS_KEY_ID` | AWS 접근 키 |
| `AWS_SECRET_ACCESS_KEY` | AWS 비밀 키 |
| `AWS_SESSION_TOKEN` | 임시 자격증명 사용 시 세션 토큰 |
| `BEDROCK_CHAT_MODEL_ID` | Chat inference profile ID 또는 ARN |
| `BEDROCK_EMBEDDING_MODEL_ID` | Embedding Model ID |

기본 Chat Model:

```text
apac.anthropic.claude-3-haiku-20240307-v1:0
```

기본 Embedding Model:

```text
amazon.titan-embed-text-v2:0
```

### 3.4 PGvector 설정

설정:

```yaml
spring.ai.vectorstore.pgvector:
  initialize-schema: true
  index-type: HNSW
  distance-type: COSINE_DISTANCE
  dimensions: 1024
  schema-name: public
  table-name: vector_store
```

초기 스키마 생성 시에는 테이블이 아직 없으므로 `schema-validation`을 비활성화한다. 운영 마이그레이션으로 `vector_store` 테이블을 고정한 이후 검증 활성화를 다시 검토한다.

필요한 PostgreSQL 확장:

```text
vector
hstore
uuid-ossp
```

초기화 파일:

```text
infra/init-db.sql
```

새 Docker 볼륨에서는 컨테이너 초기화 시 자동으로 확장이 생성된다. 기존 볼륨은 확장 생성 SQL을 별도로 한 번 실행해야 한다.

### 3.5 AI 호출 재시도

파일:

```text
agent/model/AiCallExecutor.java
agent/model/AiCallFailedException.java
```

외부 AI 호출은 네트워크 오류나 일시적인 모델 장애가 발생할 수 있다. `AiCallExecutor`는 설정된 횟수만큼 재시도하고 재시도 간격을 지수적으로 늘린다.

기본 설정:

```text
최대 시도: 3회
최초 대기: 1초
대기 순서: 1초 → 2초
```

모든 시도가 실패하면 `AiCallFailedException`을 발생시킨다.

현재는 동기 Smoke와 초기 모델 연동에 사용한다. 이후 비동기 `AgentRequest` 작업에서는 실패 사유와 재시도 횟수를 DB에 기록하도록 확장한다.

### 3.6 RAG Smoke Service

파일:

```text
agent/rag/RagSmokeService.java
agent/rag/RagSmokeResult.java
agent/rag/RagSmokeRunner.java
```

`RagSmokeService`는 다음 순서로 연결 상태를 검사한다.

```text
Bedrock Chat Model 호출
→ Titan Embedding 생성
→ 샘플 문서 두 건 PGvector 저장
→ projectRoomId 필터를 적용한 유사도 검색
→ 검색 결과 반환
→ 샘플 벡터 삭제
```

샘플 문서 메타데이터:

```text
projectRoomId
ownerId
scope
documentType
documentStatus
documentVersion
deleted
pageNumber
```

`projectRoomId` 필터를 Smoke 단계부터 적용한 이유는 RAG 권한 격리를 나중에 프롬프트로 처리하지 않고 검색 계층에서 강제하기 위해서다.

Smoke Result:

```text
query
chatModelResponded
embeddingDimensions
indexedDocumentCount
matchedDocumentCount
matchedDocumentIds
```

Chat 응답 원문과 문서 원문은 로그에 남기지 않는다. 성공 여부, 임베딩 차원, 저장·검색 건수만 기록한다.

### 3.7 Smoke 실행 방법

```powershell
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_ACCESS_KEY_ID = "..."
$env:AWS_SECRET_ACCESS_KEY = "..."
$env:BEDROCK_CHAT_MODEL_ID = "apac.anthropic.claude-3-haiku-20240307-v1:0"
$env:RAG_SMOKE_ENABLED = "true"

.\gradlew.bat bootRun --args="--spring.profiles.active=local,ai"
```

`RAG_SMOKE_ENABLED=false`이면 AI 프로필을 활성화해도 시작 시 Smoke는 자동 실행되지 않는다.

---

## 4. 테스트 코드

### JSON 계약 테스트

```text
AgentAnalysisResultJsonParserTest
```

검증 항목:

- 정상 fixture 파싱
- 지원하지 않는 버전 거부
- 필수 필드 누락 거부
- confidence 범위 오류 거부
- 알 수 없는 JSON 필드 거부

### 재시도 테스트

```text
AiCallExecutorTest
```

검증 항목:

- 일시적 실패 후 성공
- 최대 횟수 초과 시 예외

### RAG Smoke 단위 테스트

```text
RagSmokeServiceTest
```

검증 항목:

- Chat Model 호출
- 1024차원 임베딩 확인
- 문서 두 건 저장
- 프로젝트 필터 검색
- Smoke 문서 삭제

---

## 5. Step 0~1 완료 상태

| 항목 | 상태 |
|---|---|
| RAG MVP 정책 문서화 | 완료 |
| 버전이 있는 JSON 계약 | 완료 |
| 구조화 결과 검증 | 완료 |
| Spring AI 의존성 | 완료 |
| Bedrock Converse 설정 | 완료 |
| Titan Embedding 설정 | 완료 |
| PGvector 자동 구성 | 완료 |
| PostgreSQL 확장 | 완료 |
| AI 프로필 분리 | 완료 |
| 호출 시간 제한 | 완료 |
| 재시도 정책 | 완료 |
| 단위 테스트 | 완료 |
| 일반 서버 실행 | 완료 |
| 실제 AWS Chat/Embedding/Vector Smoke | 완료 |

2026-06-23 실제 연결 검증 결과:

```text
Bedrock Converse Chat 응답: 성공
Titan Embedding 차원: 1024
PGvector 저장 문서: 2건
projectRoomId 필터 검색 결과: 2건
Smoke 종료 후 임시 벡터: 0건
```

---

## 6. 다음 단계

다음 구현은 로드맵 Step 2다.

```text
Document
DocumentChunk
AgentSuggestion
SuggestionEvidence
AgentRequest
```

다만 Entity를 바로 작성하기 전에 각 데이터의 식별자, 프로젝트룸 권한 연결 방식, 삭제 및 문서 버전 정책을 먼저 확정해야 한다.
