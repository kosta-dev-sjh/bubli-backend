# RAG Step 2 데이터 모델 구현 설명

## 1. Step 2의 목적

Step 1에서는 다음 연결만 확인했다.

```text
문장
→ Titan Embedding
→ PGvector 저장
→ 유사도 검색
→ Bedrock Chat 호출
```

하지만 실제 서비스에는 다음 정보도 필요하다.

```text
어떤 사용자가 올린 문서인가?
어떤 프로젝트룸의 문서인가?
현재 추출 중인가, 검색 가능한 상태인가?
몇 번째 문서 버전인가?
검색된 문장이 원문의 몇 페이지인가?
AI 요청은 진행 중인가, 실패했는가?
AI 후보를 사용자가 승인했는가?
후보를 만든 근거 문장은 무엇인가?
```

Step 2는 이 정보를 PostgreSQL 관계형 테이블로 저장하기 위한 단계다.

---

## 2. 전체 관계

```text
Resource
  └─ Document
       └─ DocumentChunk

AgentRequest
  └─ AgentSuggestion
       └─ SuggestionEvidence
            ├─ documentId  → Document
            └─ chunkId     → DocumentChunk
```

관계의 의미:

| 관계 | 의미 |
|---|---|
| Resource → Document | 사용자 자료를 RAG 처리 대상으로 등록 |
| Document → DocumentChunk | 문서를 검색 가능한 작은 단위로 분할 |
| AgentRequest → AgentSuggestion | 한 번의 AI 분석 요청이 여러 후보를 생성 |
| AgentSuggestion → SuggestionEvidence | 후보가 어떤 원문을 근거로 생성됐는지 연결 |

---

## 3. Resource와 Document의 차이

두 이름이 비슷하지만 책임이 다르다.

### Resource

사용자가 보는 자료 카드와 업무 원본이다.

```text
제목
소유자
공유 범위
프로젝트룸
다운로드 권한
댓글
파일 버전
```

### Document

Resource를 AI 검색에 사용하기 위한 처리 기록이다.

```text
텍스트 추출 상태
RAG 문서 종류
RAG 처리 버전
파일 checksum
청크 목록
PGvector 연결 상태
실패 원인
```

`Document.resourceId`로 원본 Resource와 연결한다. 현재 다른 도메인의 Entity를 직접 참조하지 않고 UUID만 저장한다.

그 이유는 RAG 처리 코드가 Resource Entity 내부 구현에 강하게 묶이지 않게 하기 위해서다.

---

## 4. 공통 시간 Entity

파일:

```text
global/entity/BaseTimeEntity.java
```

모든 Step 2 Entity가 공통으로 상속한다.

```text
createdAt
updatedAt
```

저장 직전 `@PrePersist`가 실행되어 두 시간을 설정한다.

```text
새 Entity 저장
→ onCreate()
→ createdAt 설정
→ updatedAt 설정
→ INSERT
```

수정 직전에는 `@PreUpdate`가 실행된다.

```text
Entity 값 변경
→ onUpdate()
→ updatedAt 갱신
→ UPDATE
```

---

## 5. Document

파일:

```text
resource/entity/Document.java
```

테이블:

```text
documents
```

### 주요 식별 정보

| 필드 | 의미 |
|---|---|
| `id` | RAG 문서 버전의 ID |
| `resourceId` | 원본 Resource ID |
| `projectRoomId` | 프로젝트룸 검색 범위 |
| `ownerId` | 문서 소유자 |
| `versionGroupId` | 같은 논리 문서 버전을 묶는 ID |

한 문서를 다시 업로드하면 새로운 `id`를 만들지만 `versionGroupId`는 유지한다.

```text
계약서 v1
id = A
versionGroupId = G
documentVersion = 1

계약서 v2
id = B
versionGroupId = G
documentVersion = 2
```

### 파일 정보

```text
fileName
fileType
documentType
scope
storagePath
checksum
```

`checksum`은 같은 파일의 중복 업로드를 탐지하기 위한 SHA-256 문자열이다.

### 문서 처리 상태

```text
UPLOADED
→ EXTRACTING
→ INDEXING
→ READY
```

실패:

```text
UPLOADED / EXTRACTING / INDEXING
→ FAILED
```

삭제:

```text
어떤 활성 상태
→ DELETED
```

잘못된 상태 전이를 막기 위해 Entity 메서드로만 상태를 바꾼다.

```java
document.startExtracting();
document.startIndexing();
document.markReady();
```

예를 들어 `UPLOADED` 상태에서 바로 `markReady()`를 호출하면 예외가 발생한다.

### 최신 버전

```text
documentVersion
isLatest
```

DB에는 같은 `versionGroupId`에서 `is_latest=true`인 행이 하나만 존재하도록 부분 UNIQUE INDEX가 있다.

### 낙관적 잠금

```text
rowVersion
```

`@Version`이 적용돼 같은 행을 두 요청이 동시에 수정할 때 뒤늦은 요청이 기존 값을 덮어쓰지 못하게 한다.

---

## 6. DocumentChunk

파일:

```text
resource/entity/DocumentChunk.java
```

테이블:

```text
document_chunks
```

한 문서를 검색 가능한 작은 단위로 나눈 결과다.

| 필드 | 의미 |
|---|---|
| `documentId` | 소속 Document |
| `chunkIndex` | 문서 내부 순서, 0부터 시작 |
| `content` | 실제 검색 대상 텍스트 |
| `pageNumber` | 원본 PDF 페이지 |
| `sectionTitle` | 계약 조항 또는 절 제목 |
| `tokenCount` | 청크 토큰 수 |
| `vectorStoreId` | PGvector `vector_store.id` |
| `active` | 현재 검색 가능한 청크인지 여부 |

같은 Document 안에서는 `chunkIndex`가 중복될 수 없다.

```text
document A / chunk 0
document A / chunk 1
document A / chunk 2
```

새 문서 버전이 생성되거나 문서가 삭제되면 기존 청크는 `active=false`가 된다.

이것은 관계형 DB 검색에서 오래된 청크를 제외하기 위한 상태다.

---

## 7. 문서 버전 및 삭제 정책

파일:

```text
resource/service/DocumentLifecycleService.java
resource/service/DocumentVersionChange.java
```

### 다음 버전 생성

```text
최신 Document를 DB 잠금으로 조회
→ 기존 청크 vectorStoreId 수집
→ 기존 Document.isLatest=false
→ 기존 청크 active=false
→ 기존 버전 UPDATE를 flush
→ 새 Document 버전 INSERT
→ 제거할 vectorStoreId 반환
```

기존 버전을 먼저 `saveAndFlush()`하는 이유는 최신 버전 UNIQUE INDEX 때문이다.

새 버전을 먼저 INSERT하면 일시적으로 최신 버전이 2개가 되어 DB 제약조건을 위반할 수 있다.

### DB 잠금

Repository의 다음 조회에는 `PESSIMISTIC_WRITE` 잠금이 적용된다.

```text
findLatestVersionForUpdate()
findByIdForUpdate()
```

두 요청이 동시에 같은 문서를 새 버전으로 만드는 것을 방지한다.

### 삭제

현재 삭제는 물리적인 `DELETE`가 아니다.

```text
Document.status = DELETED
Document.isLatest = false
Document.deletedAt = 현재 시간
DocumentChunk.active = false
```

이 방식을 soft delete라고 한다.

### PGvector 삭제 책임

`DocumentLifecycleService`는 제거해야 할 `vectorStoreId` 목록을 반환한다.

```java
List<UUID> retiredVectorStoreIds
```

현재 Step 2에서는 관계형 DB 상태까지만 변경한다.

Step 5의 `DocumentIndexingService`가 이 ID를 받아 다음을 호출하게 된다.

```java
vectorStore.delete(retiredVectorStoreIds);
```

즉 Step 2는 “무엇을 삭제해야 하는가”를 결정하고, Step 5는 실제 vector를 삭제한다.

---

## 8. AgentRequest

파일:

```text
agent/entity/AgentRequest.java
```

테이블:

```text
agent_requests
```

AI 작업 한 번의 실행 상태를 기록한다.

### 요청 종류

```text
PROJECT_INIT
CONTRACT_CHECK
REQUIREMENT_ANALYSIS
WORK_CANDIDATE
DOCUMENT_DRAFT
```

### 요청 상태

```text
QUEUED
→ PROCESSING
→ COMPLETED
```

실패했지만 재시도 가능:

```text
PROCESSING
→ QUEUED
```

최대 재시도 초과:

```text
PROCESSING
→ FAILED
```

### 재시도 정보

```text
retryCount
maxRetries
errorMessage
```

`failOrQueueRetry()`는 실패 횟수를 증가시키고 남은 재시도 횟수에 따라 `QUEUED` 또는 `FAILED`를 결정한다.

### requestPayload

기능마다 입력값이 다르기 때문에 `JSONB`로 저장한다.

예:

```json
{
  "documentIds": ["..."],
  "comparisonItems": ["PAYMENT", "DELIVERY_DATE"]
}
```

### requestFingerprint

동일 요청의 중복 실행을 막기 위한 SHA-256 값이다.

`QUEUED` 또는 `PROCESSING` 상태의 같은 fingerprint는 DB UNIQUE INDEX로 중복되지 않게 한다.

---

## 9. AgentSuggestion

파일:

```text
agent/entity/AgentSuggestion.java
```

테이블:

```text
agent_suggestions
```

AI가 생성한 결과는 확정값이 아니라 후보로 저장한다.

### 후보 종류

```text
PROJECT_INFO
CONTRACT_CHECK
REQUIREMENT
WBS
TODO
CONFIRMATION_QUESTION
DOCUMENT_DRAFT
```

### 후보 상태

```text
PENDING
→ APPROVED
→ MODIFIED
→ ON_HOLD
→ REJECTED
```

정확히는 `PENDING` 또는 `MODIFIED` 상태에서 사용자가 최종 검토 상태로 이동한다.

### 원본과 수정본 분리

```text
originalContentJson
contentJson
```

예:

```text
AI 원본 금액: 3,000,000원
사용자 수정 금액: 3,200,000원
```

저장:

```json
originalContentJson = {"amount": "3000000"}
contentJson         = {"amount": "3200000"}
status              = "MODIFIED"
```

따라서 AI가 처음 생성한 값과 사용자가 수정한 값을 모두 추적할 수 있다.

### confidence

```text
0.0 ~ 1.0
```

범위를 벗어나면 Java와 DB 양쪽에서 거부한다.

---

## 10. SuggestionEvidence

파일:

```text
agent/entity/SuggestionEvidence.java
```

테이블:

```text
suggestion_evidences
```

AI 후보와 원문 근거를 연결한다.

| 필드 | 의미 |
|---|---|
| `suggestionId` | 근거가 속한 후보 |
| `documentId` | 원본 Document |
| `chunkId` | 검색된 DocumentChunk |
| `fileName` | 사용자에게 보여줄 파일명 |
| `pageNumber` | 원본 페이지 |
| `evidenceText` | 근거 문장 |
| `similarityScore` | 검색 유사도 |

같은 후보에 같은 chunk를 중복 연결할 수 없다.

후보 상세 화면은 이 데이터를 사용해 다음처럼 표시할 수 있다.

```text
계약 금액: 3,000,000원

근거:
contract.pdf 3페이지
"본 계약의 총 금액은 금 삼백만원으로 한다."
유사도: 0.9123456
```

---

## 11. JSONB 사용 범위

JSONB로 저장하는 필드:

```text
AgentRequest.requestPayload
AgentSuggestion.originalContentJson
AgentSuggestion.contentJson
```

관계형 컬럼으로 저장하는 값:

```text
ID
프로젝트룸
사용자
문서 종류
상태
버전
페이지
신뢰도
검토자
시간
```

기준은 다음과 같다.

```text
검색·필터·관계·상태 관리에 사용하는 값
→ 일반 컬럼

기능마다 구조가 달라지는 AI 세부 결과
→ JSONB
```

모든 데이터를 JSONB 하나에 넣지 않는 이유는 프로젝트 권한, 상태, 최신 버전 같은 조건을 DB가 안정적으로 검사해야 하기 때문이다.

---

## 12. Repository

### DocumentRepository

주요 기능:

```text
삭제되지 않은 문서 조회
최신 버전 조회
프로젝트룸 READY 문서 조회
문서 종류별 조회
checksum 중복 검사
최신 버전 잠금 조회
```

### DocumentChunkRepository

주요 기능:

```text
문서의 활성 청크를 순서대로 조회
최신 버전의 활성 청크 조회
vectorStoreId로 청크 조회
```

### AgentRequestRepository

주요 기능:

```text
fingerprint로 실행 중인 중복 요청 조회
프로젝트룸별 요청 상태 조회
```

### AgentSuggestionRepository

주요 기능:

```text
프로젝트룸별 검토 상태 조회
AI 요청별 후보 조회
후보 종류별 승인 결과 조회
```

### SuggestionEvidenceRepository

주요 기능:

```text
후보의 근거 목록 조회
문서와 연결된 근거 조회
```

---

## 13. Flyway 마이그레이션

파일:

```text
db/migration/V4__rag_step2_data_model.sql
```

Flyway는 애플리케이션 시작 시 아직 실행하지 않은 SQL 파일을 순서대로 실행한다.

현재 이력:

```text
Version 1: 기존 DB baseline
Version 2: RAG Step 2 data model
```

V2가 생성하는 테이블:

```text
documents
document_chunks
agent_requests
agent_suggestions
suggestion_evidences
```

마이그레이션에는 다음도 포함된다.

```text
외래 키
UNIQUE 제약조건
CHECK 제약조건
검색 인덱스
최신 버전 부분 UNIQUE INDEX
실행 중 요청 fingerprint UNIQUE INDEX
```

이미 적용된 V2 SQL은 이후 직접 수정하지 않는다. 다음 DB 변경은 `V3__...sql`을 새로 작성해야 한다.

---

## 14. 테스트

### DocumentTest

```text
UPLOADED → EXTRACTING → INDEXING → READY
삭제 시 chunk 비활성화
새 버전 생성 시 versionGroup 유지
잘못된 상태 전이 거부
```

### DocumentLifecycleServiceTest

```text
기존 버전을 flush한 뒤 새 버전 저장
삭제할 vectorStoreId 반환
```

### AgentRequestTest

```text
재시도 가능하면 QUEUED
재시도 횟수 초과 시 FAILED
```

### AgentSuggestionTest

```text
AI 원본 JSON 보존
사용자 수정 JSON 분리
근거 연결
승인 후 재수정 방지
JSON null 값 허용
```

현재 전체 테스트:

```text
17 tests
17 passed
0 failed
```

---

## 15. Step 2 완료 상태

| 항목 | 상태 |
|---|---|
| Document Entity | 완료 |
| DocumentChunk Entity | 완료 |
| AgentRequest Entity | 완료 |
| AgentSuggestion Entity | 완료 |
| SuggestionEvidence Entity | 완료 |
| Enum | 완료 |
| Repository | 완료 |
| JSONB 범위 | 완료 |
| 문서 soft delete 정책 | 완료 |
| 문서 버전 정책 | 완료 |
| 오래된 청크 비활성화 | 완료 |
| vector 삭제 대상 반환 | 완료 |
| Flyway V2 | 완료 |
| 실제 PostgreSQL 적용 | 완료 |
| 단위 테스트 | 완료 |

---

## 16. 다음 Step 3에서 사용할 흐름

```text
PDF/TXT 업로드 요청
→ 원본 파일 저장
→ Resource 생성
→ Document.createFirstVersion()
→ DocumentRepository.save()
→ 비동기 처리 요청 생성
→ AgentRequest 또는 문서 처리 작업 실행
```

Step 3에서는 이 데이터 모델 위에 업로드 API와 로컬 파일 저장 기능을 연결한다.
