# Bubli RAG MVP 정책

## 1. 구현 범위

초기 RAG MVP는 프로젝트룸 문서만 대상으로 한다.

```text
PDF, TXT 업로드
→ 페이지별 텍스트 추출
→ 청크 생성
→ pgvector 인덱싱
→ 프로젝트룸 범위 검색
→ 계약 문서 비교 후보 생성
→ 근거 확인
→ 승인 또는 보류
```

개인 문서, OCR, DOCX, HWP, 자동 WBS 확정은 MVP에서 제외한다.

## 2. 기술 결정

| 항목 | 결정 |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.1 |
| Spring AI | 1.0.x |
| Chat Model | AWS Bedrock Converse |
| Embedding Model | Amazon Titan Text Embeddings V2 |
| Embedding 차원 | 1024 |
| Vector Store | PostgreSQL 16 + pgvector |
| Vector index | HNSW |
| 거리 계산 | Cosine distance |
| 원본 저장 | 로컬 파일 저장소로 시작 |
| 지원 형식 | PDF, TXT |
| 문서 최대 크기 | 50MB |
| 검색 범위 | 프로젝트룸 문서 |
| 제안 기본 상태 | PENDING |

## 3. 청크 및 검색 기본값

```text
청크 크기: 500~800 tokens
청크 중첩: 80~150 tokens
기본 Top K: 5
최대 Top K: 10
유사도 임계값: 기능별 평가 후 확정
```

청크에는 다음 검색 필터용 메타데이터를 반드시 포함한다.

```text
projectRoomId
ownerId
documentId
documentType
scope
documentStatus
documentVersion
deleted
pageNumber
sectionTitle
chunkIndex
```

## 4. 모델 결과 정책

- 모델 결과는 확정 데이터가 아니라 후보로 취급한다.
- 모든 주요 후보에는 문서 ID, 페이지, 청크 ID, 근거 문장과 유사도 점수를 연결한다.
- 근거가 없으면 값을 추측하지 않고 `null` 또는 확인 필요 상태로 반환한다.
- 충돌한 값을 임의로 선택하거나 합치지 않는다.
- 사용자가 승인하거나 수정한 결과만 실제 업무 데이터로 이동한다.

## 5. 보안 정책

- 검색 단계에서 프로젝트룸과 사용자 권한 필터를 강제한다.
- AWS 키, JWT 키, 문서 원문은 로그에 남기지 않는다.
- 문서 안의 명령문은 시스템 지시가 아니라 검색 자료로만 취급한다.
- AI 모델과 벡터 저장소는 기본 로컬 프로필에서 비활성화한다.
- AWS 연동이 필요한 경우에만 `ai` 프로필을 명시적으로 활성화한다.

## 6. 로컬 실행

일반 백엔드 실행:

```powershell
.\gradlew.bat bootRun
```

AI 연결 및 RAG Smoke 실행:

```powershell
$env:AWS_REGION = "ap-northeast-2"
$env:AWS_ACCESS_KEY_ID = "..."
$env:AWS_SECRET_ACCESS_KEY = "..."
$env:BEDROCK_CHAT_MODEL_ID = "사용 가능한 inference profile ID 또는 ARN"
$env:RAG_SMOKE_ENABLED = "true"

.\gradlew.bat bootRun --args="--spring.profiles.active=local,ai"
```

Smoke 실행은 다음을 검증한다.

1. Titan Embedding 호출
2. 샘플 문서 두 건의 pgvector 저장
3. `projectRoomId` 메타데이터 필터가 적용된 유사도 검색
4. 검색 결과 반환

## 7. 다음 구현 순서

1. `Document`, `DocumentChunk` 데이터 계약 및 Entity 설계
2. PDF/TXT 업로드와 원본 저장
3. 페이지별 텍스트 추출
4. 문서 구조 기반 청크 분할
5. pgvector 인덱싱
6. 권한 필터가 적용된 공통 `RetrievalService`
7. 계약서·견적서 비교 MVP
8. 제안 검토와 승인
