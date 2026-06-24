# Bubli Backend Work Handoff

Last checked: 2026-06-25 06:29 KST

이 문서는 백엔드 현재 상태를 이어받기 위한 인수인계 문서다.
작업이 끝날 때마다 이 문서의 PR 상태, 확인 결과, 다음 작업을 갱신한다.

## 현재 기준

| 항목 | 값 |
|---|---|
| 로컬 레포 | `/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend` |
| 현재 확인 브랜치 | `chore/latest-docs-2026-06-25` |
| 원격 기준 브랜치 | `develop` |
| 시작 문서 | `docs/00_BACKEND_START_HERE.md` |
| API 기준 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md` |
| DB 기준 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/09_Data-Model.md` |
| Tauri 로컬 DB 기준 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/09C_DB-Tauri-SQLite.md` |
| 백엔드 규칙 | `docs/Bubli_백엔드_개발_가이드_2026-06-25.md` |
| 6/25 기준 보정 계획 | `docs/CURRENT_API_BASELINE_WORK.md` |

## 현재 작업 모드

상태: 2026-06-25 최신 문서 묶음을 기준으로 기존 PR과 현재 코드를 재검토한다.

2026-06-25에 최신 `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`, 백엔드 개발 가이드가 들어왔다.
6/24 기준으로 만든 #19~#28 PR은 닫지 않고, 새 기준과 차이 나는 부분만 후속 보정 PR로 처리한다.

현재 브랜치는 #28 `feature/testcontainers-ci-foundation` 위의 `chore/latest-docs-2026-06-25`다.
이번 브랜치는 문서와 자동화 기준 갱신만 다룬다.
코드 보정은 이 PR 이후 새 기준으로 한 PR씩 처리한다.

현재 컴파일 기준:

- `./gradlew compileTestJava` 통과 (2026-06-24 21:44 KST)
- `./gradlew cleanTest test` 통과 (2026-06-24 21:44 KST)
- `git diff --check` 통과 (2026-06-24 21:44 KST)
- GitHub Actions CI 통과 (#28, 2026-06-24 21:47 KST)
- GitHub Actions CI 통과 (#29, 2026-06-25 00:26 KST)
- GitHub Actions CI 통과 (#19, 2026-06-25 00:55 KST)
- GitHub Actions CI 통과 (#20, 2026-06-25 01:01 KST)
- GitHub Actions CI 통과 (#21, 2026-06-25 01:06 KST)
- GitHub Actions CI 통과 (#21, WBS board/reorder 보정 후 2026-06-25 01:50 KST)
- #30 time-log 기본 API 로컬 검증 통과. GitHub checks 없음 (base #21에 stacked PR CI workflow 없음)
- #25 resource comment API 로컬 검증 통과. GitHub checks 없음 (base #24에 stacked PR CI workflow 없음)
- #25 resource version API 로컬 검증 통과. GitHub checks 없음 (base #24에 stacked PR CI workflow 없음)
- #25 resource summary API, `ResourceSummaryStatus`, 자료 삭제 정책 6/25 기준 보정 로컬 검증 통과. GitHub checks 없음 (base #24에 stacked PR CI workflow 없음)
- #31 resource related API 로컬 검증 통과. GitHub checks 없음 (base #25에 stacked PR CI workflow 없음)
- #32 agent job status API 로컬 검증 통과. GitHub checks 없음 (base #26에 stacked PR CI workflow 없음)
- #33 agent suggestion list API 로컬 검증 통과. GitHub checks 없음 (base #32에 stacked PR CI workflow 없음)
- #34 agent job events API 로컬 검증 통과. GitHub checks 없음 (base #33에 stacked PR CI workflow 없음)
- #35 agent suggestion update API 로컬 검증 통과. GitHub checks 없음 (base #34에 stacked PR CI workflow 없음)
- #36 resource ai-document API 로컬 검증 통과. GitHub checks 없음 (base #35에 stacked PR CI workflow 없음)
- #37 room ai-documents API 로컬 검증 통과. GitHub checks 없음 (base #36에 stacked PR CI workflow 없음)
- #38 entity boundary guard 로컬 검증 통과. GitHub Actions `build` 통과
- #39 storage usage API와 accounting boundary 로컬 검증 통과. GitHub checks 없음 (base #31에 stacked PR CI workflow 없음)
- #40 analyze-resource job API 로컬 검증 통과. GitHub checks 없음 (base #37에 stacked PR CI workflow 없음)
- #41 generate-requirements job API 로컬 검증 통과. GitHub checks 없음 (base #40에 stacked PR CI workflow 없음)
- #42 generate-tasks job API 로컬 검증 통과. GitHub checks 없음 (base #41에 stacked PR CI workflow 없음)
- #43 generate-wbs job API 로컬 검증 통과. GitHub checks 없음 (base #42에 stacked PR CI workflow 없음)
- #44 generate-questions job API 로컬 검증 통과. GitHub checks 없음 (base #43에 stacked PR CI workflow 없음)
- #45 review-contract-documents job API 로컬 검증 통과. GitHub checks 없음 (base #44에 stacked PR CI workflow 없음)
- #46 resource download-url API 로컬 검증 통과. GitHub checks 없음 (base #31에 stacked PR CI workflow 없음)
- #47 user profile update API 로컬 검증 통과. GitHub Actions `build` 통과
- #48 user preferences API 로컬 검증 통과. GitHub checks 없음 (base #47에 stacked PR CI workflow 없음)
- #49 user notification preferences API 로컬 검증 통과. GitHub checks 없음 (base #48에 stacked PR CI workflow 없음)
- #50 user privacy consents API 로컬 검증 통과. GitHub checks 없음 (base #49에 stacked PR CI workflow 없음)
- #51 user project rooms API 로컬 검증 통과. GitHub checks 없음 (base #50에 stacked PR CI workflow 없음)
- #52 S3 download-url provider 로컬 검증 통과. GitHub checks 없음 (base #46에 stacked PR CI workflow 없음)
- #53 agent job dispatch boundary, in-memory queue adapter 초안, enqueue 실패 기록 로컬 검증 통과. GitHub checks 없음 (base #45에 stacked PR CI workflow 없음)
- #54 S3 storage service boundary 로컬 검증 통과. GitHub checks 없음 (base #52에 stacked PR CI workflow 없음)
- #55 resource multipart upload API 로컬 검증 통과. GitHub checks 없음 (base #54에 stacked PR CI workflow 없음)
- #56 resource upload compensation 로컬 검증 통과. GitHub checks 없음 (base #55에 stacked PR CI workflow 없음)
- #57 resource upload policy 로컬 검증 통과. GitHub checks 없음 (base #56에 stacked PR CI workflow 없음)
- #27 agent 핵심 테이블 Flyway 컬럼/타입 고정 테스트 보강. 로컬 검증 통과. GitHub checks 없음
- 열린 PR #19~#57 상태 재확인 완료 (2026-06-25 06:29 KST)
- 엔티티 44개, Repository 4개, Controller 4개, Service 5개 확인
- 6/25 기준 세부 작업 지시는 `docs/CURRENT_API_BASELINE_WORK.md`를 기준으로 나눈다.

## 최근 완료 작업

### 작업 카드 49. #25 ResourceStatus 삭제 정책 보정

처리 시각: 2026-06-25 06:29 KST

변경 내용:

- #25 `feature/resource-basic-foundation` 기존 PR 브랜치를 갱신했다.
- `ResourceStatus.DELETED`를 제거했다.
- `Resource.markDeleted`가 status를 바꾸지 않고 `deleted_at`만 기록하도록 보정했다.
- 삭제 테스트를 `deleted_at` 중심으로 바꾸고 기존 status가 유지되는지 확인하게 했다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: head `36b9b55`, base `feature/auth-google-foundation`, mergeState `CLEAN`
- #25: GitHub checks 없음. #28 workflow 보강 전 생성된 stacked draft PR이다.

메모:

- 6/25 API 문서의 "삭제 대신 상태만 바꾸는 처리는 두지 않는다"는 기준에 맞춰 삭제 상태값을 제거했다.
- 삭제된 자료는 기존 repository 조회 조건의 `deleted_at is null`로 목록/상세에서 제외한다.
- 물리 파일 삭제, storage usage 해제, S3 객체 정리는 upload/storage stack 정렬 뒤 별도 PR로 분리한다.

### 작업 카드 48. #25 ResourceSummaryStatus 6/25 기준 보정

처리 시각: 2026-06-25 06:24 KST

변경 내용:

- #25 `feature/resource-basic-foundation` 기존 PR 브랜치를 갱신했다.
- `ResourceSummaryStatus`를 6/25 데이터 모델 기준의 `READY`, `ANALYZING`, `ANALYZED`, `FAILED`로 보정했다.
- 기존 테스트에서 완료 요약 상태 기대값을 `SUCCEEDED`에서 `ANALYZED`로 바꿨다.
- 자료 댓글, 버전, summary endpoint 구조는 건드리지 않았다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: head `af3a04c`, base `feature/auth-google-foundation`, mergeState `CLEAN`
- #25: GitHub checks 없음. #28 workflow 보강 전 생성된 stacked draft PR이다.

메모:

- 이번 변경은 `resource_summaries.status` enum 값 보정만 다룬다.
- `ResourceStatus.DELETED`와 `deleted_at` 조합은 6/25 삭제 정책과 최종 API 기준에서 별도 확인한다.
- resource upload flow에서 storage usage accounting boundary를 호출하는 작업은 #39/#57 stack 정렬 뒤 별도 PR로 처리한다.

### 작업 카드 47. #27 agent 핵심 테이블 Flyway 타입 검증

처리 시각: 2026-06-25 06:17 KST

변경 내용:

- #27 `feature/entity-flyway-alignment` 기존 PR 브랜치를 갱신했다.
- `EntityFlywayAlignmentTest`에 agent 핵심 테이블 컬럼 타입 검증을 추가했다.
- 대상 테이블은 `ai_documents`, `agent_jobs`, `agent_job_events`, `agent_model_call_logs`, `agent_suggestions`다.
- UUID, VARCHAR 길이, JSONB, TIMESTAMPTZ, NUMERIC, BIGINT, INTEGER, TEXT 타입을 Flyway V1 기준으로 고정했다.
- 운영 코드와 API 응답은 바꾸지 않았다.

검증 결과:

- #27: `./gradlew compileTestJava` 통과
- #27: `./gradlew cleanTest test` 통과
- #27: `git diff --check` 통과
- #27: head `66e2586`, base `feature/agent-storage-foundation`, mergeState `CLEAN`
- #27: GitHub checks 없음. #28 workflow 보강 전 생성된 stacked draft PR이다.

메모:

- 이번 변경은 source/schema 파일 기반 타입 검증까지 다룬다.
- FK, 인덱스, enum 값 검증은 Docker/Testcontainers 기반 검증 또는 별도 parser 보강 PR로 분리한다.

### 작업 카드 46. #53 agent dispatch enqueue 실패 기록

처리 시각: 2026-06-25 06:12 KST

변경 내용:

- #53 `feature/agent-job-dispatch-boundary` 기존 PR 브랜치를 갱신했다.
- `AgentJobDispatchFailureRecorder`를 추가해 dispatch enqueue 실패를 `agent_jobs`의 `FAILED` 상태로 기록한다.
- `AgentJobDispatchEventListener`가 AFTER_COMMIT dispatch 실패를 잡고 실패 recorder를 호출한다.
- 실패 기록은 별도 `REQUIRES_NEW` 트랜잭션으로 처리한다.
- 실제 retry, worker 실행, `RUNNING` 상태 전환은 추가하지 않았다.
- agent가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하는 흐름은 추가하지 않았다.

검증 결과:

- #53: `./gradlew compileTestJava` 통과
- #53: `./gradlew cleanTest test` 통과
- #53: `git diff --check` 통과
- #53: head `8059952`, base `feature/review-contract-documents-job-api`, mergeState `CLEAN`
- #53: GitHub checks 없음. base #45에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 enqueue 실패를 job 상태로 남기는 초안만 다룬다.
- retry 정책, failed event 저장, outbox/Redis queue 연동, worker 실행은 후속 PR로 분리한다.

### 작업 카드 45. #53 agent dispatch queue adapter 초안

처리 시각: 2026-06-25 06:04 KST

변경 내용:

- #53 `feature/agent-job-dispatch-boundary` 기존 PR 브랜치를 갱신했다.
- `AgentJobQueueMessage`를 추가해 dispatch command를 queue payload로 변환한다.
- `agent.dispatch.adapter=in-memory` 설정에서 활성화되는 `InMemoryAgentJobQueueAdapter`를 추가했다.
- adapter는 queue에 message를 넣기만 하고 실제 모델 실행이나 업무 데이터 확정 저장은 하지 않는다.
- 기본 설정에서는 기존 `NoopAgentJobDispatchPort`가 유지된다.

검증 결과:

- #53: `./gradlew compileTestJava` 통과
- #53: `./gradlew cleanTest test` 통과
- #53: `git diff --check` 통과
- #53: head `878e004`, base `feature/review-contract-documents-job-api`, mergeState `CLEAN`
- #53: GitHub checks 없음. base #45에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 실제 Redis/worker 연동 전 adapter 초안만 다룬다.
- enqueue 실패 처리, retry, `RUNNING` 상태 전환, 실제 모델 실행은 후속 PR로 분리한다.
- agent가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하는 흐름은 추가하지 않았다.

### 작업 카드 44. #39 storage usage accounting boundary

처리 시각: 2026-06-25 05:59 KST

변경 내용:

- #39 `feature/storage-usage-api` 기존 PR 브랜치를 갱신했다.
- `StorageUsageService`에 개인/프로젝트룸 업로드 사용량 기록 메서드를 추가했다.
- `StorageUsageService`에 개인/프로젝트룸 사용량 해제 메서드를 추가했다.
- `storage_usage` row가 없으면 설정 기반 기본 quota로 생성하는 경계를 추가했다.
- `storage.default-personal-limit-bytes`, `storage.default-room-limit-bytes` 설정을 사용한다.
- 사용량이 `limit_bytes`를 넘으면 `STORAGE_400_002`로 거절한다.
- 조회 API endpoint와 응답 DTO는 바꾸지 않았다.

검증 결과:

- #39: `./gradlew compileTestJava` 통과
- #39: `./gradlew cleanTest test` 통과
- #39: `git diff --check` 통과
- #39: head `282a7d1`, base `feature/resource-related-api`, mergeState `CLEAN`
- #39: GitHub checks 없음. base #31에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 storage usage accounting Service 경계만 다룬다.
- resource upload flow에서 이 boundary를 호출하는 위치는 #57 이후 upload stack 보정 PR로 분리한다.
- 실제 quota 기본값, 에러 코드 세분화, quota row 선생성 정책은 최종 API 수정본에서 보정 가능하다.

### 작업 카드 43. #57 자료 업로드 정책 검사

처리 시각: 2026-06-25 05:51 KST

변경 내용:

- #57 `feature/resource-upload-policy`를 #56 `feature/resource-upload-compensation` 위의 draft stacked PR로 생성했다.
- `storage.max-upload-size-bytes` 설정으로 업로드 최대 크기를 검사한다. 기본값은 100MB다.
- `storage.allowed-mime-types` 설정이 비어 있으면 모든 MIME 타입을 허용하고, 값이 있으면 쉼표 구분 allow-list로 검사한다.
- API endpoint와 응답 DTO는 바꾸지 않았다.

검증 결과:

- #57: `./gradlew compileTestJava` 통과
- #57: `./gradlew cleanTest test` 통과
- #57: `git diff --check` 통과
- #57: head `756174e`, base `feature/resource-upload-compensation`, mergeState `CLEAN`
- #57: GitHub checks 없음. base #56에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 실제 최대 파일 크기 기본값과 허용 MIME 타입 목록은 최종 API 수정본 또는 운영 정책에 맞춰 보정 가능하다.
- resource upload flow에서 storage usage accounting boundary를 호출하는 작업은 별도 PR로 분리한다.
- 업로드 정책 에러 코드를 세분화할지는 최종 API 응답 규칙에 맞춰 보정한다.

### 작업 카드 42. #56 자료 업로드 실패 보상 삭제

처리 시각: 2026-06-25 05:46 KST

변경 내용:

- #56 `feature/resource-upload-compensation`을 #55 `feature/resource-multipart-upload-api` 위의 draft stacked PR로 생성했다.
- `StorageService.save` 성공 뒤 DB 메타데이터 저장 또는 버전 저장이 실패하면 `StorageService.delete`로 업로드된 객체를 정리한다.
- 보상 삭제 자체가 실패하면 원래 예외에 suppressed exception으로 붙이고 원래 실패를 그대로 전파한다.
- API endpoint와 응답 DTO는 바꾸지 않았다.

검증 결과:

- #56: `./gradlew compileTestJava` 통과
- #56: `./gradlew cleanTest test` 통과
- #56: `git diff --check` 통과
- #56: head `b18efb6`, base `feature/resource-multipart-upload-api`, mergeState `CLEAN`
- #56: GitHub checks 없음. base #55에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 저장소와 DB 사이 실패 처리만 다룬다.
- 파일 크기 제한, 허용 MIME 타입, 업로드 상태값 `UPLOADING` 선저장 여부는 후속 보정으로 남긴다.
- 보상 삭제 실패에 대한 로깅/재시도/운영 알림 정책은 최종 API 수정본 또는 운영 기준이 정해지면 보강한다.

### 작업 카드 41. #55 자료 multipart 업로드 API 연결

처리 시각: 2026-06-25 05:42 KST

변경 내용:

- #55 `feature/resource-multipart-upload-api`를 #54 `feature/s3-storage-service-boundary` 위의 draft stacked PR로 생성했다.
- 기존 JSON 메타데이터 생성 API는 유지하고, `POST /api/resources`의 `multipart/form-data` handler를 추가했다.
- 업로드 시 `resources` row를 생성하고 `StorageService`에 파일 저장을 위임한다.
- 저장 결과를 `resource_files`에 기록하고 `resource_versions` v1을 생성한다.
- 응답은 Entity를 직접 반환하지 않고 기존 `ResourceResponse` DTO를 사용한다.
- `docs/http/resource.http`에 개인/프로젝트룸 multipart 업로드 예시를 추가했다.

검증 결과:

- #55: `./gradlew compileTestJava` 통과
- #55: `./gradlew cleanTest test` 통과
- #55: `git diff --check` 통과
- #55: head `c186557`, base `feature/s3-storage-service-boundary`, mergeState `CLEAN`
- #55: GitHub checks 없음. base #54에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 현재 API 기준 자료 업로드 연결이다.
- multipart field 이름, 응답 DTO에 최신 file/version 정보를 포함할지 여부, 파일 크기 제한과 허용 MIME 타입은 최종 API 수정본에서 보정 가능하다.
- 저장소 업로드 성공 후 DB 저장 실패 시 보상 삭제 정책은 후속 안정화 PR에서 다룰 수 있다.

### 작업 카드 40. #54 S3 저장 서비스 경계

처리 시각: 2026-06-25 05:33 KST

변경 내용:

- #54 `feature/s3-storage-service-boundary`를 #52 `feature/s3-download-url-provider` 위의 draft stacked PR로 생성했다.
- `StorageService` 인터페이스와 S3 저장/삭제 구현을 추가했다.
- `FileUploadResult`로 `storageKey`, 원본 파일명, MIME 타입, 크기, SHA-256 checksum을 반환한다.
- `storage.type=s3`일 때 `S3Client`와 `S3StorageService`가 활성화된다.
- 저장소 미연결 환경에서는 `DisabledStorageService`가 `RESOURCE_501_002`를 반환한다.
- 실제 multipart 업로드 API endpoint는 추가하지 않았다.

검증 결과:

- #54: `./gradlew compileTestJava` 통과
- #54: `./gradlew cleanTest test` 통과
- #54: `git diff --check` 통과
- #54: head `ae6eed7`, base `feature/s3-download-url-provider`, mergeState `CLEAN`
- #54: GitHub checks 없음. base #52에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 업로드 API가 아니라 저장소 Service 경계만 다룬다.
- 파일 크기 제한, 허용 MIME 타입, 업로드 실패 보상 삭제 정책은 후속 안정화 PR에서 다룬다.
- S3 객체는 public으로 열지 않고, 권한 확인 후 download-url API에서 presigned URL을 발급하는 구조를 유지한다.

### 작업 카드 39. #27 agent 테이블 Entity/Flyway 정합성 보강

처리 시각: 2026-06-25 05:25 KST

변경 내용:

- #27 `feature/entity-flyway-alignment` 기존 PR 브랜치를 갱신했다.
- `EntityFlywayAlignmentTest`에 6/25 데이터 모델 기준 agent 핵심 테이블 컬럼 집합 검증을 추가했다.
- 대상 테이블은 `ai_documents`, `agent_jobs`, `agent_job_events`, `agent_model_call_logs`, `agent_suggestions`다.
- handoff에 남아 있던 `agent_model_call_logs` 정합성 의심은 실제 Flyway/Entity/데이터 모델 비교 결과 맞는 것으로 확인했다.

검증 결과:

- #27: `./gradlew compileTestJava` 통과
- #27: `./gradlew cleanTest test` 통과
- #27: `git diff --check` 통과
- #27: head `3c96ed4`, base `feature/agent-storage-foundation`, mergeState `CLEAN`
- #27: GitHub checks 없음. base #27에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 테스트 보강만 다룬다.
- 타입, FK, 인덱스, enum 값까지의 전체 검증은 후속 Testcontainers/Flyway 검증에서 보강 가능하다.

### 작업 카드 38. #53 에이전트 작업 dispatch 경계

처리 시각: 2026-06-25 05:19 KST

변경 내용:

- #53 `feature/agent-job-dispatch-boundary`를 #45 `feature/review-contract-documents-job-api` 위의 draft stacked PR로 생성했다.
- AgentJob 생성 후 실제 실행을 직접 호출하지 않고 dispatch boundary로 넘기는 구조를 추가했다.
- `AgentJobDispatchPort`와 기본 no-op 구현을 추가했다.
- dispatch는 Spring event로 발행하고 listener가 transaction commit 이후 port를 호출한다.
- agent가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하는 로직은 추가하지 않았다.

검증 결과:

- #53: `./gradlew compileTestJava` 통과
- #53: `./gradlew cleanTest test` 통과
- #53: `git diff --check` 통과
- #53: head `1884979`, base `feature/review-contract-documents-job-api`, mergeState `CLEAN`
- #53: GitHub checks 없음. base #45에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 실행 큐 연결 전 경계만 다룬다.
- 실제 큐 adapter, retry/enqueue 실패 처리, 모델 실행 흐름은 후속 PR로 분리한다.
- job 실행 결과를 suggestion/ai_document로 저장하는 세부 흐름은 최종 API 수정본에서 보정 가능하다.

### 작업 카드 37. #52 S3 다운로드 URL Provider

처리 시각: 2026-06-25 05:12 KST

변경 내용:

- #52 `feature/s3-download-url-provider`를 #46 `feature/resource-download-url-api` 위의 draft stacked PR로 생성했다.
- #46의 `StorageDownloadUrlProvider` 경계에 S3 presigned download URL 구현을 추가했다.
- `storage.type=s3`일 때만 `S3StorageDownloadUrlProvider`와 `S3Presigner` Bean이 활성화된다.
- `storage.type=local` 등 기본 환경에서는 기존 disabled provider가 유지된다.
- S3 객체는 public으로 열지 않고, 서버 권한 확인 후 presigned URL만 발급하는 흐름을 유지한다.
- 테스트는 실제 AWS 호출 없이 로컬 서명 URL 생성과 bucket 누락 방어를 검증한다.

검증 결과:

- #52: `./gradlew compileTestJava` 통과
- #52: `./gradlew cleanTest test` 통과
- #52: `git diff --check` 통과
- #52: head `0de5a0a`, base `feature/resource-download-url-api`, mergeState `CLEAN`
- #52: GitHub checks 없음. base #46에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 download-url Provider 구현만 다룬다.
- S3 업로드/삭제까지 포함한 통합 StorageService 구현은 별도 PR로 분리한다.
- presigned URL 만료 시간과 Content-Disposition 정책은 최종 API 수정본에서 보정 가능하다.

### 작업 카드 36. #51 내 프로젝트룸 목록 API

처리 시각: 2026-06-25 05:06 KST

변경 내용:

- #51 `feature/user-project-rooms-api`를 #50 `feature/user-privacy-consents-api` 위의 draft stacked PR로 생성했다.
- `GET /api/me/project-rooms`를 추가했다.
- 기존 `GET /api/project-rooms`와 같은 `ProjectRoomService.getProjectRooms` 조회 기준을 사용한다.
- 로그인 사용자의 active room membership 기준으로 접근 가능한 프로젝트룸만 반환한다.
- 응답은 Entity를 직접 반환하지 않고 `ProjectRoomResponse` DTO를 사용한다.
- `docs/http/user.http`에 내 프로젝트룸 목록 수동 검증 예시를 추가했다.

검증 결과:

- #51: `./gradlew compileTestJava` 통과
- #51: `./gradlew cleanTest test` 통과
- #51: `git diff --check` 통과
- #51: head `8650257`, base `feature/user-privacy-consents-api`, mergeState `CLEAN`
- #51: GitHub checks 없음. base #50에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 `/api/me/project-rooms` alias API만 다룬다.
- `/api/project-rooms`와 `/api/me/project-rooms`를 둘 다 유지할지는 최종 API 수정본에서 보정 가능하다.
- 내가 만든 프로젝트룸과 참여 중인 프로젝트룸을 별도 구분해야 하면 응답 DTO를 후속 보정한다.

### 작업 카드 35. #50 사용자 개인정보 동의 API

처리 시각: 2026-06-25 05:00 KST

변경 내용:

- #50 `feature/user-privacy-consents-api`를 #49 `feature/user-notification-preferences-api` 위의 draft stacked PR로 생성했다.
- `GET /api/me/privacy-consents`, `PATCH /api/me/privacy-consents`를 추가했다.
- `user_privacy_consents` 복합키 엔티티에 생성/수정 메서드를 붙였다.
- 저장된 동의 row가 없으면 민감 기능은 기본 미동의로 내려준다.
- `PATCH`는 요청에 들어온 동의 종류만 upsert한다.
- 응답은 Entity를 직접 반환하지 않고 `UserPrivacyConsentResponse` DTO를 사용한다.
- `docs/http/user.http`에 개인정보 동의 조회/수정 수동 검증 예시를 추가했다.

검증 결과:

- #50: `./gradlew compileTestJava` 통과
- #50: `./gradlew cleanTest test` 통과
- #50: `git diff --check` 통과
- #50: head `bb44f55`, base `feature/user-notification-preferences-api`, mergeState `CLEAN`
- #50: GitHub checks 없음. base #49에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 사용자별 privacy-consents API만 다룬다.
- `ACTIVITY_CONTEXT`, `MANAGED_FOLDER` 외 동의 종류가 최종 API에서 추가되면 후속 보정한다.
- 동의 변경 이력 테이블이 필요해지면 별도 PR로 분리한다.

### 작업 카드 34. #49 사용자 알림 설정 API

처리 시각: 2026-06-25 04:53 KST

변경 내용:

- #49 `feature/user-notification-preferences-api`를 #48 `feature/user-preferences-api` 위의 draft stacked PR로 생성했다.
- `GET /api/me/notification-preferences`, `PATCH /api/me/notification-preferences`를 추가했다.
- `user_notification_preferences` 복합키 엔티티에 생성/수정 메서드를 붙였다.
- 저장된 알림 설정 row가 없으면 `NotificationType` 전체를 기본 켜짐으로 내려준다.
- `PATCH`는 요청에 들어온 알림 종류만 upsert한다.
- 응답은 Entity를 직접 반환하지 않고 `UserNotificationPreferenceResponse` DTO를 사용한다.
- `docs/http/user.http`에 알림 설정 조회/수정 수동 검증 예시를 추가했다.

검증 결과:

- #49: `./gradlew compileTestJava` 통과
- #49: `./gradlew cleanTest test` 통과
- #49: `git diff --check` 통과
- #49: head `f94239e`, base `feature/user-preferences-api`, mergeState `CLEAN`
- #49: GitHub checks 없음. base #48에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 사용자별 notification-preferences API만 다룬다.
- `RESOURCE`, `STORAGE` enum 이름이 최종 API에서 더 구체화되면 후속 보정한다.
- 알림 기본값을 row 생성 없이 `true`로 볼지, 모든 row를 명시 생성할지는 최종 API 수정본에서 보정 가능하다.
- 사용자 설정 API 묶음 중 `PATCH /api/me`, preferences, notification-preferences, privacy-consents까지 보정 완료했다.

### 작업 카드 33. #48 사용자 설정 API

처리 시각: 2026-06-25 04:43 KST

변경 내용:

- #48 `feature/user-preferences-api`를 #47 `feature/user-me-update-api` 위의 draft stacked PR로 생성했다.
- `GET /api/me/preferences`, `PATCH /api/me/preferences`를 추가했다.
- 사용자별 `theme`, `defaultHomeType`, `defaultProjectRoomId`를 조회/수정한다.
- `defaultProjectRoomId`가 들어오면 기존 `ProjectRoomService.getProjectRoom`으로 접근 권한을 확인한다.
- `user_preferences`의 `default_room_id`를 6/25 추천 이름인 `default_project_room_id`로 맞추는 V2 마이그레이션을 추가했다.
- 응답은 Entity를 직접 반환하지 않고 `UserPreferenceResponse` DTO를 사용한다.
- `docs/http/user.http`에 사용자 설정 조회/수정 수동 검증 예시를 추가했다.

검증 결과:

- #48: `./gradlew compileTestJava` 통과
- #48: `./gradlew cleanTest test` 통과
- #48: `git diff --check` 통과
- #48: head `bbad8ae`, base `feature/user-me-update-api`, mergeState `CLEAN`
- #48: GitHub checks 없음. base #47에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 사용자별 preferences API만 다룬다.
- `theme`, `defaultHomeType`의 허용값 enum화와 기본 프로젝트룸 해제 요청 형식은 최종 API 수정본에서 보정 가능하다.
- 사용자별 알림 설정과 개인정보 동의 API는 #49, #50에서 후속 보정 완료했다.

### 작업 카드 32. #47 내 프로필 수정 API

처리 시각: 2026-06-25 04:36 KST

변경 내용:

- #47 `feature/user-me-update-api`를 `develop` 기준 draft PR로 생성했다.
- `PATCH /api/me`를 추가했다.
- 로그인 사용자의 이름, 프로필 이미지 URL, 언어, 시간대를 `users` 엔티티에서 수정한다.
- 응답은 Entity를 직접 반환하지 않고 `MeResponse` DTO를 사용한다.
- Service에는 Request DTO가 아니라 `UpdateUserProfileCommand`를 전달한다.
- `docs/http/user.http`에 내 프로필 조회/수정 수동 검증 예시를 추가했다.

검증 결과:

- #47: `./gradlew compileTestJava` 통과
- #47: `./gradlew cleanTest test` 통과
- #47: `git diff --check` 통과
- #47: head `2a132a6`, base `develop`, mergeState `BLOCKED`
- #47: GitHub Actions `build` 통과. run `28124495298`, job `83284670388`, duration `1m18s`

메모:

- 이번 변경은 6/25 기준 사용자별 설정 API 중 `PATCH /api/me`만 다룬다.
- null/빈 문자열을 필드 삭제로 볼지 변경 없음으로 볼지는 최종 API 수정본에서 보정 가능하다.
- `GET/PATCH /api/me/preferences`, 알림 설정, 개인정보 동의 API는 후속 PR로 남긴다.

### 작업 카드 31. #46 resource download-url API 뼈대

처리 시각: 2026-06-25 04:26 KST

변경 내용:

- #46 `feature/resource-download-url-api`를 #31 `feature/resource-related-api` 위의 draft stacked PR로 생성했다.
- `GET /api/resources/{resourceId}/download-url`를 추가했다.
- 자료 읽기 권한을 확인한 뒤 최신 `resource_versions`와 연결된 `resource_files.storage_key`를 조회한다.
- 실제 S3 presigned URL을 지어내지 않고 `StorageDownloadUrlProvider` 경계로 분리했다.
- 기본 Provider는 저장소 URL 발급 미연결 상태를 `RESOURCE_501_001`로 반환한다.
- 응답은 Entity를 직접 반환하지 않고 `ResourceDownloadUrlResponse` DTO를 사용한다.
- `docs/http/resource.http`에 자료 다운로드 URL 발급 수동 검증 예시를 추가했다.

검증 결과:

- #46: `./gradlew compileTestJava` 통과
- #46: `./gradlew cleanTest test` 통과
- #46: `git diff --check` 통과
- #46: head `5e70334`, base `feature/resource-related-api`, mergeState `CLEAN`
- #46: GitHub checks 없음. base #31에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 download-url API 표면과 저장소 Provider 경계만 다룬다.
- S3 presigned URL 구현, 만료 시간 정책, 특정 버전 다운로드 지원 여부는 후속 PR로 남긴다.
- 가짜 다운로드 URL을 만들지 않았다.

### 작업 카드 30. #45 review-contract-documents 에이전트 작업 생성 API

처리 시각: 2026-06-25 04:20 KST

변경 내용:

- #45 `feature/review-contract-documents-job-api`를 #44 `feature/generate-questions-job-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/review-contract-documents`를 추가했다.
- 프로젝트룸 ACTIVE 멤버 권한을 확인한 뒤 `REVIEW_CONTRACT_DOCUMENTS` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, 계약서/요구사항 문서 추출 및 비교, 확인 질문 후보 저장은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 계약서 문서 검토 작업 수동 검증 예시를 추가했다.

검증 결과:

- #45: `./gradlew compileTestJava` 통과
- #45: `./gradlew cleanTest test` 통과
- #45: `git diff --check` 통과
- #45: head `8fefcee`, base `feature/generate-questions-job-api`, mergeState `CLEAN`
- #45: GitHub checks 없음. base #44에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 계약서 문서 검토 작업 생성만 다룬다.
- 요청 본문의 계약서/요구사항 resourceId 목록은 아직 과확정하지 않았다.
- job 실행 큐 연결과 `agent_suggestions` 후보 저장 payload는 후속 PR로 남긴다.

### 작업 카드 29. #44 generate-questions 에이전트 작업 생성 API

처리 시각: 2026-06-25 04:16 KST

변경 내용:

- #44 `feature/generate-questions-job-api`를 #43 `feature/generate-wbs-job-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/generate-questions`를 추가했다.
- 프로젝트룸 ACTIVE 멤버 권한을 확인한 뒤 `GENERATE_QUESTIONS` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, 확인 질문 후보 저장, 업무 데이터 확정 저장은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 확인 질문 후보 생성 작업 수동 검증 예시를 추가했다.

검증 결과:

- #44: `./gradlew compileTestJava` 통과
- #44: `./gradlew cleanTest test` 통과
- #44: `git diff --check` 통과
- #44: head `a8eea88`, base `feature/generate-wbs-job-api`, mergeState `CLEAN`
- #44: GitHub checks 없음. base #43에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 확인 질문 후보 생성 작업 생성만 다룬다.
- agent가 업무 데이터를 직접 확정 저장하는 흐름은 넣지 않았다.
- job 실행 큐 연결과 `agent_suggestions` 후보 저장 payload는 후속 PR로 남긴다.

### 작업 카드 28. #43 generate-wbs 에이전트 작업 생성 API

처리 시각: 2026-06-25 04:12 KST

변경 내용:

- #43 `feature/generate-wbs-job-api`를 #42 `feature/generate-tasks-job-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/generate-wbs`를 추가했다.
- 프로젝트룸 ACTIVE 멤버 권한을 확인한 뒤 `GENERATE_WBS` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, WBS 후보 저장, `wbs_items` 확정 저장은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 WBS 후보 생성 작업 수동 검증 예시를 추가했다.

검증 결과:

- #43: `./gradlew compileTestJava` 통과
- #43: `./gradlew cleanTest test` 통과
- #43: `git diff --check` 통과
- #43: head `7429dec`, base `feature/generate-tasks-job-api`, mergeState `CLEAN`
- #43: GitHub checks 없음. base #42에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 WBS 후보 생성 작업 생성만 다룬다.
- agent가 `wbs_items`를 직접 확정 저장하는 흐름은 넣지 않았다.
- job 실행 큐 연결과 `agent_suggestions` 후보 저장 payload는 후속 PR로 남긴다.

### 작업 카드 27. #42 generate-tasks 에이전트 작업 생성 API

처리 시각: 2026-06-25 04:08 KST

변경 내용:

- #42 `feature/generate-tasks-job-api`를 #41 `feature/generate-requirements-job-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/generate-tasks`를 추가했다.
- 프로젝트룸 ACTIVE 멤버 권한을 확인한 뒤 `GENERATE_TASKS` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, TODO 후보 저장, 업무 데이터 확정 저장은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 TODO 후보 생성 작업 수동 검증 예시를 추가했다.

검증 결과:

- #42: `./gradlew compileTestJava` 통과
- #42: `./gradlew cleanTest test` 통과
- #42: `git diff --check` 통과
- #42: head `7a25da0`, base `feature/generate-requirements-job-api`, mergeState `CLEAN`
- #42: GitHub checks 없음. base #41에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 TODO 후보 생성 작업 생성만 다룬다.
- agent가 `tasks`를 직접 확정 저장하는 흐름은 넣지 않았다.
- job 실행 큐 연결과 `agent_suggestions` 후보 저장 payload는 후속 PR로 남긴다.

### 작업 카드 26. #41 generate-requirements 에이전트 작업 생성 API

처리 시각: 2026-06-25 04:03 KST

변경 내용:

- #41 `feature/generate-requirements-job-api`를 #40 `feature/analyze-resource-job-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/generate-requirements`를 추가했다.
- 프로젝트룸 ACTIVE 멤버 권한을 확인한 뒤 `GENERATE_REQUIREMENTS` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, 요구사항 확정 저장, `agent_suggestions` 저장은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 요구사항 후보 생성 작업 수동 검증 예시를 추가했다.

검증 결과:

- #41: `./gradlew compileTestJava` 통과
- #41: `./gradlew cleanTest test` 통과
- #41: `git diff --check` 통과
- #41: head `9c12eae`, base `feature/analyze-resource-job-api`, mergeState `CLEAN`
- #41: GitHub checks 없음. base #40에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 요구사항 후보 생성 작업 생성만 다룬다.
- agent가 요구사항이나 할 일을 직접 확정 저장하는 흐름은 넣지 않았다.
- job 실행 큐 연결과 `agent_suggestions` 후보 저장 payload는 후속 PR로 남긴다.

### 작업 카드 25. #40 analyze-resource 에이전트 작업 생성 API

처리 시각: 2026-06-25 03:59 KST

변경 내용:

- #40 `feature/analyze-resource-job-api`를 #37 `feature/room-ai-documents-api` 위의 draft stacked PR로 생성했다.
- `POST /api/ai/analyze-resource`를 추가했다.
- `ResourceService.getResource`로 자료 읽기 권한을 먼저 확인한 뒤 `ANALYZE_RESOURCE` agent job을 `PENDING` 상태로 생성한다.
- 실제 모델 실행, 요약/임베딩/AI 문서 생성, 후보 생성은 아직 실행하지 않는다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO를 사용한다.
- `docs/http/agent.http`에 자료 분석 작업 생성 수동 검증 예시를 추가했다.

검증 결과:

- #40: `./gradlew compileTestJava` 통과
- #40: `./gradlew cleanTest test` 통과
- #40: `git diff --check` 통과
- #40: head `769a707`, base `feature/room-ai-documents-api`, mergeState `CLEAN`
- #40: GitHub checks 없음. base #37에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 자료 분석 작업 생성만 다룬다.
- agent가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하는 흐름은 넣지 않았다.
- 분석 실행 큐 연결, `resource_summaries`, `resource_embeddings`, `ai_documents`, `agent_suggestions` 갱신은 후속 PR로 남긴다.

### 작업 카드 24. #39 storage usage 조회 API

처리 시각: 2026-06-25 03:52 KST

변경 내용:

- #39 `feature/storage-usage-api`를 #31 `feature/resource-related-api` 위의 draft stacked PR로 생성했다.
- `GET /api/storage/usage`를 추가했다.
- 현재 사용자 개인 `storage_usage`와 ACTIVE로 참여 중인 프로젝트룸의 ROOM scope `storage_usage`를 함께 조회한다.
- 각 usage row의 `remainingBytes`와 전체 `totalUsedBytes`, `totalLimitBytes`, `totalRemainingBytes`를 계산해 반환한다.
- 응답은 Entity를 직접 반환하지 않고 `StorageUsageResponse`와 `StorageUsageResult` 계열 DTO로 분리했다.
- `docs/http/resource.http`에 저장 용량 조회 수동 검증 예시를 추가했다.

검증 결과:

- #39: `./gradlew compileTestJava` 통과
- #39: `./gradlew cleanTest test` 통과
- #39: `git diff --check` 통과
- #39: head `db757bc`, base `feature/resource-related-api`, mergeState `CLEAN`
- #39: GitHub checks 없음. base #31에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 저장 용량 조회만 다룬다.
- 파일 업로드, resource download-url, LocalFileStorage/S3Storage 구현은 별도 PR로 남긴다.

### 작업 카드 23. #38 엔티티 경계 가드 테스트

처리 시각: 2026-06-25 03:40 KST

변경 내용:

- #38 `chore/entity-boundary-guards`를 #28 `feature/testcontainers-ci-foundation` 위의 draft stacked PR로 생성했다.
- `EntityBoundaryGuardTest`를 추가했다.
- 서버 코드에 `BaseTimeEntity.java`가 생기면 테스트가 실패하도록 했다.
- `global/entity` 아래 Java 소스가 생기면 테스트가 실패하도록 했다.
- 서버 JPA Entity가 Tauri 로컬 SQLite용 `local_*` 테이블에 매핑되면 테스트가 실패하도록 했다.
- 빈 로컬 디렉터리는 Git 추적 대상이 아니라 위반으로 보지 않고, 실제 Java 소스만 검사한다.

검증 결과:

- #38: 최초 `./gradlew cleanTest test`는 빈 `global/entity` 디렉터리를 위반으로 오탐해 1회 실패했다.
- #38: 테스트 기준을 실제 Java 소스 기준으로 보정했다.
- #38: `./gradlew compileTestJava` 통과
- #38: `./gradlew cleanTest test` 통과
- #38: `git diff --check` 통과
- #38: head `a22ee45`, base `feature/testcontainers-ci-foundation`, mergeState `CLEAN`
- #38: GitHub Actions `build` 통과, 1m28s

메모:

- 이번 변경은 금지된 엔티티 경계를 테스트로 고정하는 작업만 다룬다.
- 애플리케이션 코드, Entity, Flyway, Gradle, PR 템플릿, README, SETUP은 건드리지 않았다.

### 작업 카드 22. #37 project-room ai-documents 목록 조회 API

처리 시각: 2026-06-25 03:30 KST

변경 내용:

- #37 `feature/room-ai-documents-api`를 #36 `feature/resource-ai-document-api` 위의 draft stacked PR로 생성했다.
- `GET /api/project-rooms/{roomId}/ai-documents`를 추가했다.
- 프로젝트룸 AI 문서 목록은 `RoomAccessService.validateActiveMember`로 ACTIVE 멤버만 조회할 수 있게 했다.
- `status` query parameter를 선택값으로 두고, 없으면 룸 전체 AI 문서를 조회한다.
- 기본 정렬은 `updatedAt desc`, `id desc`로 두었다.
- 응답은 Entity를 직접 반환하지 않고 `AiDocumentResponse` DTO 페이지로 감싼다.
- `docs/http/agent.http`에 프로젝트룸 AI 문서 목록 수동 검증 예시를 추가했다.

검증 결과:

- #37: `./gradlew compileTestJava` 통과
- #37: `./gradlew cleanTest test` 통과
- #37: `git diff --check` 통과
- #37: head `aa52ec1`, base `feature/resource-ai-document-api`, mergeState `CLEAN`
- #37: GitHub checks 없음. base #36에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 프로젝트룸 AI 문서 목록 조회만 다룬다.
- AI 문서 생성/분석 실행은 후속 PR로 남긴다. resource download-url과 S3 presigned URL Provider는 #46, #52에서 보정 완료했다.

### 작업 카드 21. #36 resource ai-document 조회 API

처리 시각: 2026-06-25 03:24 KST

변경 내용:

- #36 `feature/resource-ai-document-api`를 #35 `feature/agent-suggestion-update-api` 위의 draft stacked PR로 생성했다.
- `GET /api/resources/{resourceId}/ai-document`를 추가했다.
- `ResourceService.getResource`로 자료 읽기 권한을 먼저 확인한 뒤 `ai_documents` 단건 분석 상태를 조회한다.
- `AiDocumentController`를 별도 파일로 추가해 기존 `ResourceController` 변경 충돌 가능성을 줄였다.
- 응답은 Entity를 직접 반환하지 않고 `AiDocumentResponse` DTO로 감싼다.
- AI 문서가 없으면 `AGENT_404_003`으로 응답한다.
- `docs/http/agent.http`에 자료 AI 문서 조회 수동 검증 예시를 추가했다.

검증 결과:

- #36: `./gradlew compileTestJava` 통과
- #36: `./gradlew cleanTest test` 통과
- #36: `git diff --check` 통과
- #36: head `d8bea2b`, base `feature/agent-suggestion-update-api`, mergeState `CLEAN`
- #36: GitHub checks 없음. base #35에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 자료의 AI 문서 분석 상태 조회만 다룬다.
- `GET /api/project-rooms/{roomId}/ai-documents` 목록 조회와 AI 문서 생성/분석 실행은 후속 PR로 남긴다.
- resource download-url/S3 presigned URL은 실제 Storage Service가 생긴 뒤 별도 PR에서 다룬다.

### 작업 카드 20. #35 agent_suggestions 상태/내용 수정 API

처리 시각: 2026-06-25 03:12 KST

변경 내용:

- #35 `feature/agent-suggestion-update-api`를 #34 `feature/agent-job-events-api` 위의 draft stacked PR로 생성했다.
- `PATCH /api/agent/suggestions/{suggestionId}`를 추가했다.
- 개인 제안은 제안 소유자만 수정할 수 있게 했다.
- 프로젝트룸 제안은 `RoomAccessService.validateActiveMember`로 ACTIVE 멤버만 수정할 수 있게 했다.
- 제안 `status`, `payloadJson`, `evidenceJson`을 수정할 수 있게 했다.
- 요청은 `UpdateAgentSuggestionRequest`, 서비스 입력은 `UpdateAgentSuggestionCommand`로 분리했다.
- 응답은 Entity를 직접 반환하지 않고 `AgentSuggestionResponse` DTO로 감싼다.
- `docs/http/agent.http`에 제안 수정 수동 검증 예시를 추가했다.

검증 결과:

- #35: `./gradlew compileTestJava` 통과
- #35: `./gradlew cleanTest test` 통과
- #35: `git diff --check` 통과
- #35: head `f27b0ce`, base `feature/agent-job-events-api`, mergeState `CLEAN`
- #35: GitHub checks 없음. base #34에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 agent suggestion의 상태/내용 수정만 다룬다.
- `APPROVED` 상태로 바꿀 수는 있지만, `tasks`, `wbs_items`, `schedules`, `memos` 같은 확정 업무 데이터는 생성하지 않는다.
- 후보 승인 후 실제 업무 데이터 반영은 target 도메인 Service가 맡는 후속 PR로 남긴다.

### 작업 카드 19. #34 agent_job_events 조회 API

처리 시각: 2026-06-25 02:54 KST

변경 내용:

- #34 `feature/agent-job-events-api`를 #33 `feature/agent-suggestion-list-api` 위의 draft stacked PR로 생성했다.
- `GET /api/agent-jobs/{jobId}/events`를 추가했다.
- 로그인 사용자 본인이 요청한 `agent_jobs`의 이벤트만 조회한다.
- `agent_job_events`를 기준으로 이벤트 목록을 페이지 응답으로 반환한다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobEventResponse` DTO로 감싼다.
- `docs/http/agent.http`에 작업 이벤트 조회 수동 검증 예시를 추가했다.

검증 결과:

- #34: `./gradlew compileTestJava` 통과
- #34: `./gradlew cleanTest test` 통과
- #34: `git diff --check` 통과
- #34: head `698557a`, base `feature/agent-suggestion-list-api`, mergeState `CLEAN`
- #34: GitHub checks 없음. base #33에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 agent job event 조회만 다룬다.
- event 생성/저장 API는 에이전트 실행 모듈과 상태 전이 정책이 정리되면 후속 PR에서 다룬다.
- agent가 tasks, wbs_items, schedules, memos를 직접 확정 저장하는 흐름은 넣지 않았다.

### 작업 카드 18. #33 agent_suggestions 조회 API

처리 시각: 2026-06-25 02:49 KST

변경 내용:

- #33 `feature/agent-suggestion-list-api`를 #32 `feature/agent-job-status-api` 위의 draft stacked PR로 생성했다.
- `GET /api/agent/suggestions`를 추가했다.
- `GET /api/project-rooms/{roomId}/agent/suggestions`를 추가했다.
- 기본 `status`는 `DRAFT`로 두고 페이지 응답으로 반환한다.
- 프로젝트룸 제안함은 `RoomAccessService.validateActiveMember`로 ACTIVE 멤버만 조회하게 했다.
- 응답은 Entity를 직접 반환하지 않고 `AgentSuggestionResponse` DTO로 감싼다.
- `docs/http/agent.http`에 개인/프로젝트룸 제안함 수동 검증 예시를 추가했다.

검증 결과:

- #33: `./gradlew compileTestJava` 통과
- #33: `./gradlew cleanTest test` 통과
- #33: `git diff --check` 통과
- #33: head `a863439`, base `feature/agent-job-status-api`, mergeState `CLEAN`
- #33: GitHub checks 없음. base #32에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 agent suggestion 조회만 다룬다.
- `PATCH /api/agent/suggestions/{id}` 승인/수정/보류/삭제 API는 확정 저장 책임이 커서 후속 PR로 남긴다.
- agent가 tasks, wbs_items, schedules, memos를 직접 확정 저장하는 흐름은 넣지 않았다.

### 작업 카드 17. #32 agent_jobs 상태 조회 API

처리 시각: 2026-06-25 02:44 KST

변경 내용:

- #32 `feature/agent-job-status-api`를 #26 `feature/agent-storage-foundation` 위의 draft stacked PR로 생성했다.
- `GET /api/agent-jobs/{jobId}`를 추가했다.
- 로그인 사용자 본인이 요청한 `agent_jobs`만 조회한다.
- 존재하지 않거나 본인 job이 아니면 `AGENT_404_001`로 응답한다.
- 응답은 Entity를 직접 반환하지 않고 `AgentJobResponse` DTO로 감싼다.
- `docs/http/agent.http`에 작업 상태 조회 수동 검증 예시를 추가했다.

검증 결과:

- #32: `./gradlew compileTestJava` 통과
- #32: `./gradlew cleanTest test` 통과
- #32: `git diff --check` 통과
- #32: head `d667612`, base `feature/agent-storage-foundation`, mergeState `CLEAN`
- #32: GitHub checks 없음. base #26에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 agent job 상태 조회만 다룬다.
- `POST /api/ai/*` 작업 생성 API는 권한, 분석 제한, 큐 연결 정책이 더 필요해서 후속 PR로 남긴다.
- agent가 tasks, wbs_items, schedules, memos를 직접 확정 저장하는 흐름은 넣지 않았다.

### 작업 카드 16. #31 resource_relations 조회 API

처리 시각: 2026-06-25 02:40 KST

변경 내용:

- #31 `feature/resource-related-api`를 #25 `feature/resource-basic-foundation` 위의 draft stacked PR로 생성했다.
- `GET /api/resources/{resourceId}/related`를 추가했다.
- `resource_relations`를 기준으로 관련 자료 목록을 조회한다.
- 기준 자료와 관련 자료 모두 사용자 접근 권한을 확인한다.
- 응답은 Entity를 직접 반환하지 않고 `ResourceRelatedResponse` DTO로 감싼다.
- `docs/http/resource.http`에 관련 자료 목록 수동 검증 예시를 추가했다.

검증 결과:

- #31: `./gradlew compileTestJava` 통과
- #31: `./gradlew cleanTest test` 통과
- #31: `git diff --check` 통과
- #31: head `4148e57`, base `feature/resource-basic-foundation`, mergeState `CLEAN`
- #31: GitHub checks 없음. base #25에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 관련 자료 조회만 다룬다.
- 관련 자료 관계 생성/갱신은 에이전트 분석 저장 정책이 정리된 뒤 별도 PR에서 다룬다.
- `download-url`, `ai-document` API는 아직 후속 작업이다.

### 작업 카드 15. #25 resource_summaries 조회 API 보정

처리 시각: 2026-06-25 02:30 KST

변경 내용:

- #25 `feature/resource-basic-foundation`에 resource summary 조회 API를 추가했다.
- `GET /api/resources/{id}/summary`를 추가했다.
- 자료 접근 권한을 먼저 확인한 뒤 최신 `resource_summaries` 행을 반환한다.
- 최신 요약은 `updatedAt desc`, `id desc` 기준으로 고른다.
- 응답은 Entity를 직접 반환하지 않고 `ResourceSummaryResponse` DTO로 감싼다.
- `docs/http/resource.http`에 summary 조회 수동 검증 예시를 추가했다.
- #25 PR 본문을 새 head, 변경 내용, 로컬 검증, checks 없음 사유 기준으로 갱신했다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: head `c01a0fc`, base `feature/auth-google-foundation`, mergeState `CLEAN`
- #25: GitHub checks 없음. base #24에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 read-only summary 조회만 다룬다.
- summary 생성/갱신은 에이전트 분석 저장 흐름이 정리되면 별도 PR에서 다룬다.
- `download-url`, `ai_documents`, `related` API는 아직 후속 작업이다.
- #25 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 14. #25 resource_versions 기본 API 보정

처리 시각: 2026-06-25 02:21 KST

변경 내용:

- #25 `feature/resource-basic-foundation`에 resource version API를 추가했다.
- `GET /api/resources/{id}/versions`, `POST /api/resources/{id}/versions`를 추가했다.
- 파일 메타데이터는 `resource_files`, 버전 이력은 `resource_versions`를 원본으로 저장한다.
- 새 버전 등록 시 현재 최대 `version_no` 다음 번호를 부여한다.
- 버전 목록 응답에는 버전 정보와 연결 파일 메타데이터를 DTO로 함께 반환한다.
- `docs/http/resource.http`에 버전 수동 검증 예시를 추가했다.
- #25 PR 본문을 새 head, 변경 내용, 로컬 검증, checks 없음 사유 기준으로 갱신했다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: head `f2343bc`, base `feature/auth-google-foundation`, mergeState `CLEAN`
- #25: GitHub checks 없음. base #24에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.
- #26: #25 최신 head 반영 후 GitHub mergeState `CLEAN` 확인

메모:

- 실제 multipart 업로드와 S3 presigned URL 발급은 아직 후속 작업이다.
- `resource_summaries`, `ai_documents`, `related` API는 아직 후속 작업이다.
- #25 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 13. #25 resource_comments 기본 API 보정

처리 시각: 2026-06-25 02:13 KST

변경 내용:

- #25 `feature/resource-basic-foundation`에 resource comment API를 추가했다.
- `GET /api/resources/{id}/comments`, `POST /api/resources/{id}/comments`를 추가했다.
- `PATCH /api/resource-comments/{id}`, `DELETE /api/resource-comments/{id}`를 추가했다.
- 댓글 목록/작성은 자료 접근 권한을 먼저 확인한다.
- 댓글 수정/삭제는 작성자만 가능하게 했다.
- 삭제는 `resource_comments.deleted_at` soft delete로 처리한다.
- `docs/http/resource.http`에 댓글 수동 검증 예시를 추가했다.
- #25 PR 본문을 새 head, 변경 내용, 로컬 검증, checks 없음 사유 기준으로 갱신했다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: head `7837d32`, base `feature/auth-google-foundation`, mergeState `CLEAN`
- #25: GitHub checks 없음. base #24에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 이번 변경은 `resource_comments`만 다룬다.
- `download-url`, `resource_versions`, `resource_summaries`, `ai_documents`, `related` API는 아직 후속 작업이다.
- #25 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 12. time_logs 기본 API 추가

처리 시각: 2026-06-25 02:06 KST

변경 내용:

- #30 `feature/time-log-basic-api`를 #21 `feature/work-task-wbs-api` 위의 stacked PR로 생성했다.
- `POST /api/time-logs/start`를 추가했다.
- `PATCH /api/time-logs/{id}/pause`, `resume`, `stop`, `heartbeat`를 추가했다.
- `idempotencyKey` 기준으로 중복 시작 요청은 기존 time_log를 반환하게 했다.
- 일반 타이머와 작업 타이머를 `timer_type`으로 구분한다.
- 작업 타이머가 `room_id`와 연결되면 `room_members.status=ACTIVE` 기준으로 권한을 검사한다.
- Tauri `local_timer_state`는 서버 JPA 엔티티로 만들지 않았다.
- `docs/http/personal.http`에 타이머 수동 검증 예시를 추가했다.

검증 결과:

- #30: `./gradlew compileTestJava` 통과
- #30: `./gradlew cleanTest test` 통과
- #30: `git diff --check` 통과
- #30: head `f162377`, base `feature/work-task-wbs-api`, mergeState `CLEAN`
- #30: GitHub checks 없음. base #21에는 #28의 `feature/**` stacked PR CI 보강이 아직 포함되지 않았다.

메모:

- 현재 DB 기준 `time_logs`에는 `task_id`, `wbs_item_id`가 없으므로 TODO/WBS 직접 연결은 넣지 않았다.
- 타이머 복구 정책은 Tauri `local_timer_state`와 앱 복구 UX가 확정되면 보강한다.
- #30 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 11-3. #21 WBS board/reorder 보정

처리 시각: 2026-06-25 01:51 KST

변경 내용:

- #21 `feature/work-task-wbs-api`에 `GET /api/project-rooms/{roomId}/wbs-board`를 추가했다.
- WBS board 응답은 WBS 항목 목록과 프로젝트룸 TODO 목록을 DTO로 함께 반환한다.
- #21에 `PATCH /api/project-rooms/{roomId}/wbs-items/reorder`를 추가했다.
- reorder 요청은 같은 부모 아래 순서 중복, 자기 자신을 parent로 지정하는 경우, 같은 방 밖 parent 지정 시도를 검증한다.
- reorder 저장 시 DB unique 제약 충돌을 피하도록 임시 순번 적용 후 최종 순번을 저장한다.
- `docs/http/work.http`에 WBS board/reorder 수동 검증 예시를 추가했다.
- PR 본문을 새 head, 변경 내용, 로컬 검증, CI 결과 기준으로 갱신했다.

검증 결과:

- #21: `./gradlew compileTestJava` 통과
- #21: `./gradlew cleanTest test` 통과
- #21: `git diff --check` 통과
- #21: head `5f232da`, base `develop`, GitHub Actions `build` pass, 1m12s

메모:

- 이번 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.
- time-log API는 #21에 섞지 않고 별도 PR로 분리한다.
- #21은 여전히 base `develop` 기준 merge blocked 상태다.

### 작업 카드 11-2. #26~#28 base 충돌 정리와 CI 재확인

처리 시각: 2026-06-25 01:38 KST

변경 내용:

- #26 `feature/agent-storage-foundation`에 #25 `feature/resource-basic-foundation` 최신 변경을 병합했다.
- #27 `feature/entity-flyway-alignment`에 #26 `feature/agent-storage-foundation` 최신 변경을 병합했다.
- #28 `feature/testcontainers-ci-foundation`에 #27 `feature/entity-flyway-alignment` 최신 변경을 병합했다.
- 각 병합에서 발생한 `docs/WORK_HANDOFF.md` 충돌은 해당 PR 기록을 모두 보존하도록 정리했다.
- #26, #27, #28 PR 본문을 최신 head, mergeState, 검증 결과 기준으로 갱신했다.

검증 결과:

- #26: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과
- #26: head `1382c41`, base `feature/resource-basic-foundation`, mergeState `CLEAN`, GitHub checks 없음
- #27: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과
- #27: head `3c96ed4`, base `feature/agent-storage-foundation`, mergeState `CLEAN`, GitHub checks 없음
- #28: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과
- #28: head `810ec58`, base `feature/entity-flyway-alignment`, mergeState `CLEAN`, GitHub Actions `build` 통과

메모:

- #26, #27은 workflow 보강 전 생성된 draft stacked PR이라 GitHub checks가 없다.
- #28은 CI workflow를 포함하는 PR이라 새 head에서 GitHub Actions가 다시 실행됐고 통과했다.
- #29는 #28 최신 base 병합 후 문서 PR CI를 다시 확인해야 한다.

### 작업 카드 11-1. #25 base 충돌 정리와 stack 상태 재확인

처리 시각: 2026-06-25 01:23 KST

변경 내용:

- #25 `feature/resource-basic-foundation`에 #24 `feature/auth-google-foundation` 최신 변경을 병합했다.
- `docs/WORK_HANDOFF.md` 충돌을 #24 auth 기록과 #25 resource 기록이 모두 남도록 해결했다.
- #25 PR 본문을 최신 head, mergeState, 로컬 검증 결과 기준으로 갱신했다.
- 열린 PR #19~#29의 head, base, mergeState, CI 상태를 다시 확인했다.

검증 결과:

- #25: `./gradlew compileTestJava` 통과
- #25: `./gradlew cleanTest test` 통과
- #25: `git diff --check` 통과
- #25: GitHub checks 없음. workflow 보강 전 생성된 draft stacked PR이다.
- #25: head `14c522d`, base `feature/auth-google-foundation`, mergeState `CLEAN`

메모:

- #26 `feature/agent-storage-foundation`은 #25 base 갱신 후 mergeState `DIRTY`로 확인됐다.
- #27 `feature/entity-flyway-alignment`도 mergeState `DIRTY`로 확인됐다.
- 사용자 지시의 “git 충돌이 생기면 멈춤” 기준에 따라 #26/#27 병합 정리는 진행하지 않고 여기서 중단한다.
- #24/#25 변경에는 Gradle, GitHub Actions, PR 템플릿, README 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 11. 6/25 기준 열린 PR 보정 1차

처리 시각: 2026-06-25 00:58 KST

변경 내용:

- #19 `feature/project-room-members-invitations`에 초대 취소, 멤버 역할 변경, 멤버 제거/나가기 API를 추가했다.
- #20 `feature/chat-basic-api`에 `POST /api/chat/direct-rooms`를 추가했다.
- #21 `feature/work-task-wbs-api`에 `GET /api/dashboard/tasks`를 추가했다.
- #24 `feature/auth-google-foundation`에 Google authorize/callback endpoint 뼈대를 반영하고 `POST /api/auth/login` 뼈대를 제거했다.
- #25 `feature/resource-basic-foundation`에 `PATCH /api/resources/{id}`, `DELETE /api/resources/{id}`를 추가했다.
- #26 `feature/agent-storage-foundation`에 6/25 기준 agent suggestion/job enum을 확장했다.
- #19, #20, #24, #26 PR 본문을 현재 변경 내용과 검증 결과 기준으로 갱신했다.

검증 결과:

- #19: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check`, GitHub Actions `build` pass
- #20: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check`, GitHub Actions `build` pass
- #21: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check`, GitHub Actions `build` pass
- #24: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과. GitHub checks 없음
- #25: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과. GitHub checks 없음. 후속 11-1에서 base 충돌 정리 완료
- #26: `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과. GitHub checks 없음

메모:

- #24와 #26은 workflow 보강 전 생성된 draft stacked PR이라 GitHub checks가 없다.
- #25도 workflow 보강 전 생성된 draft stacked PR이라 GitHub checks가 없다. #24 최신 base 병합 충돌은 후속 11-1에서 해결했다.
- #24 변경에는 `SecurityConfig`의 auth permit 경로 보정이 포함된다. Gradle, GitHub Actions, PR 템플릿, README 같은 초기 개발환경 세팅 파일은 건드리지 않았다.
- #19 브랜치에는 `docs/WORK_HANDOFF.md`가 없어서 새 파일을 추가하지 않았다. #19 결과는 PR 본문과 이 최신 handoff 문서에 기록했다.

### 작업 카드 10. 2026-06-25 최신 문서 기준 반영

처리 시각: 2026-06-25 00:25 KST

변경 내용:

- `/Users/maren/Downloads`의 최신 6/25 문서 4개를 기준으로 반영했다.
- 위키 레포 `p3-bubli.wiki`의 `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`를 최신본으로 갱신했다.
- 최종 산출물 폴더에 `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, 최신 `10_API-Design.md`를 모았다.
- 백엔드 레포에는 `docs/Bubli_백엔드_개발_가이드_2026-06-25.md`를 추가했다.
- 기존 `docs/Bubli_백엔드_개발_가이드_2026-06-24.md`, `docs/API_PRE_FINAL_TASKS.md`는 `docs/archive/2026-06-24/`로 보존했다.
- 다운로드 원본 백엔드 가이드는 `docs/archive/2026-06-25-downloads/Bubli_백엔드_개발_가이드_2026-06-25.original.md`에 보존했다.
- `docs/00_BACKEND_START_HERE.md`, `docs/CODEX_BACKEND_WORKFLOW.md`, `docs/CURRENT_API_BASELINE_WORK.md`, 로컬 `bubli-backend-workflow` 스킬을 6/25 기준으로 갱신했다.

검증 결과:

- Downloads 원본과 반영 파일 shasum 비교 완료
- 활성 문서의 구 기준 참조 검색 완료
- 백엔드 레포 `git diff --check` 통과
- 위키 레포 `git diff --check` 통과
- GitHub Actions CI 통과: #29 `build` pass, 1m6s

메모:

- 6/25 기준 문서 반영 PR 이후 #19~#28을 새 기준으로 재검토한다.
- 이번 작업은 문서/자동화 기준 갱신이며 Java 코드는 수정하지 않는다.
- 백엔드 PR: https://github.com/kosta-dev-sjh/bubli-backend/pull/29

### 작업 카드 9. Testcontainers/CI 테스트 기반 보강

처리 시각: 2026-06-24 21:47:05 KST

변경 내용:

- `.github/workflows/ci.yml`의 `pull_request` base branch에 `feature/**`를 추가했다.
- feature 브랜치를 base로 하는 stacked PR에도 GitHub Actions CI가 붙도록 했다.
- CI에 PR diff whitespace check를 추가했다.
- CI 명령을 로컬 완료 기준과 맞춰 `./gradlew compileTestJava`, `./gradlew cleanTest test`로 분리했다.
- `actions/checkout`에 `fetch-depth: 0`을 지정해 PR base diff check가 가능하게 했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions CI 통과: #28 `build` pass, 1m21s

메모:

- #28은 이번 stacked PR 흐름 중 실제 GitHub Actions check가 붙고 통과한 PR이다.
- #28이 기준 브랜치에 포함되면 이후 feature 기반 stacked PR도 CI check가 붙는다.
- #23~#27은 workflow 보강 전 생성된 PR이라 checks 없음 상태로 남아 있으며, merge stack이 정리된 뒤 `develop` 기준 CI를 다시 확인해야 한다.

### 작업 카드 8. Entity/Flyway 정합성 점검

처리 시각: 2026-06-24 21:42:53 KST

변경 내용:

- `EntityFlywayAlignmentTest`를 추가했다.
- 엔티티의 `@Table/@Column` 선언과 `V1__init_schema.sql`의 `CREATE TABLE` 컬럼을 대조한다.
- Docker/Testcontainers가 꺼져 통합 테스트가 스킵되는 환경에서도 기본적인 테이블/컬럼 누락을 잡을 수 있게 했다.
- `@EmbeddedId` 기반 복합키는 별도 `id` 컬럼으로 오해하지 않도록 제외했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24, #25, #26 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 현재 기준 테스트에서는 엔티티 테이블/컬럼 누락이 발견되지 않았다.
- 이 테스트는 타입, FK, 인덱스, enum 값까지 검증하지 않는다.
- Docker가 켜진 환경의 Testcontainers/Flyway validate 보강은 다음 카드에서 다룬다.

### 작업 카드 7-1. 6/25 기준 agent enum 보정

처리 시각: 2026-06-25 00:43 KST

변경 내용:

- 6/25 백엔드 개발 가이드와 `10_API-Design.md` 기준으로 `AgentSuggestionType`을 확장했다.
- `TODO`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY`, `MEMO` 후보 타입을 추가했다.
- 기존 데이터 모델 표에 남아 있는 `TASK`, `REVIEW_ITEM` 값은 바로 제거하지 않고 호환값으로 유지했다.
- `AgentJobType`에 요구사항 생성, 계약 문서 검토, 질문 생성, 하루정리, 자료 검색, 문서 초안 작업 타입을 추가했다.
- `AgentStorageServiceTest`의 후보 저장 검증을 6/25 기준 `TODO` 후보로 바꿨다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #26은 workflow 보강 전 생성된 stacked PR이라 #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 변경은 agent 저장 enum 보정만 다룬다.
- agent Controller/API 연결은 별도 PR로 분리한다.
- 후보 승인 후 확정 데이터 생성은 여전히 target 도메인 Service가 맡는다.

### 작업 카드 7. agent 저장 기반

처리 시각: 2026-06-24 21:39:10 KST

변경 내용:

- `AgentJob`, `AgentSuggestion`, `AiDocument` 저장 기반을 추가했다.
- `AgentJobRepository`, `AgentSuggestionRepository`, `AiDocumentRepository`를 추가했다.
- `AgentJobService`, `AgentSuggestionService`, `AiDocumentService`를 추가했다.
- Service 입력/반환은 Command/Result 객체로 분리했다.
- `AgentSuggestion`은 후보를 `DRAFT`로 저장하며, TODO/WBS/일정/메모 같은 확정 데이터는 만들지 않는다.
- agent 서비스에서 다른 도메인의 Repository나 Entity를 직접 호출하지 않았다.
- `AgentStorageServiceTest`로 job 생성, 실패 상태 전이, 후보 DRAFT 저장, AI 문서 READY 저장을 검증했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 내부 저장 기반이다. 에이전트 Controller/API 연결은 별도 PR로 분리한다.
- 후보 승인 후 확정 데이터 생성은 target 도메인 Service에서 처리해야 한다.
- `AgentSuggestionType` enum은 현재 코드 기준 후보 타입만 있으며, 최종 API/기획 기준 타입 확장이 필요할 수 있다.
- `agent_model_call_logs` 테이블 정의와 `AgentModelCallLog` 엔티티 정합성은 #27 보강에서 확인 완료했다.

### 작업 카드 6. resource 기본 저장/조회 기반

처리 시각: 2026-06-24 21:34:16 KST

변경 내용:

- 당시 `10_API-Design.md`를 기준으로 자료 기본 저장/조회 API 뼈대를 추가했다.
- `ResourceController`, `ResourceService`, `ResourceRepository`를 추가했다.
- `GET /api/resources?scope=personal`, `GET /api/project-rooms/{roomId}/resources`, `POST /api/resources`, `GET /api/resources/{resourceId}`를 구현했다.
- API 응답은 Entity를 직접 반환하지 않고 `ResourceResponse`와 `ResourceResult`로 분리했다.
- 개인 자료는 `owner_id`, 프로젝트룸 자료는 `room_id`와 ACTIVE `room_members` 기준으로 접근을 확인한다.
- `docs/http/resource.http`에 당시 API 기준 수동 검증 예시를 추가했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 자료 카드 메타데이터 저장/조회 기반이다.
- 실제 파일 업로드, S3 저장, 다운로드 URL, 버전, 댓글, 요약/AI 문서 API는 별도 PR로 분리한다.
- API 예시의 자료 상태값 `UPLOADED`, `ARCHIVED`와 당시 DB/코드 enum `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED` 사이 차이는 6/25 기준으로 다시 확인한다.

### 작업 카드 3-1. 6/25 기준 Google auth endpoint 보정

처리 시각: 2026-06-25 00:47 KST

변경 내용:

- 6/25 `10_API-Design.md` 기준에 맞춰 `POST /api/auth/login` 뼈대를 제거했다.
- `GET /api/auth/google/authorize` endpoint 뼈대를 추가했다.
- `POST /api/auth/google/callback` endpoint 뼈대를 추가했다.
- 보안 허용 경로를 Google authorize/callback, refresh 기준으로 바꿨다.
- `.http` 예시를 Google OAuth code callback 기준으로 수정했다.
- 실제 Google OAuth 검증, authorize URL 생성, refresh token rotation은 기존처럼 501 TODO로 남겼다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #24는 workflow 보강 전 생성된 stacked PR이라 #22, #23 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- signup, email/password 로그인은 되살리지 않았다.
- 이번 변경은 auth endpoint surface와 예시만 6/25 기준으로 맞춘다.
- 기초 설정, Gradle, GitHub Actions, PR 템플릿, README는 건드리지 않았다.

### 작업 카드 2. 프로젝트룸 권한 검사 서비스 분리

처리 시각: 2026-06-24 21:01:54 KST

변경 내용:

- `project.service.RoomAccessService`를 추가해 프로젝트룸 접근 권한 확인을 공통 Service로 분리했다.
- `room_members.status=ACTIVE` 확인용 `isActiveMember`, `validateActiveMember` 메서드를 제공했다.
- `PROJECT_LEADER` 권한 확인용 `isProjectLeader`, `validateProjectLeader` 메서드를 제공했다.
- `RoomMemberRepository`에 ACTIVE + role 기준 exists 메서드를 추가했다.
- `ProjectRoomService`, `ScheduleService`의 직접 ACTIVE 멤버 확인 로직을 `RoomAccessService` 호출로 바꿨다.
- `RoomAccessServiceTest`를 추가하고, 기존 `ProjectRoomServiceTest`, `ScheduleServiceTest`를 새 권한 Service 기준으로 수정했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

메모:

- 새 Controller endpoint, 초대/멤버/일정 API 요청/응답 모양은 건드리지 않았다.
- 일정 목록 조회의 JPA Specification 안에 있는 `room_members.status=ACTIVE` subquery는 조회 필터라서 유지했다.
- 이 작업은 #22 `feature/schedule-basic-api` 위에서 이어지는 foundation 작업이다.
- stacked PR로 생성하면 현재 CI 설정상 GitHub Actions가 실행되지 않을 수 있다.
- #22 merge 후 `develop` 기준으로 GitHub Actions CI를 다시 확인해야 한다.

## 열린 PR 상태

| PR | 제목 | 브랜치 | base | 확인한 head | CI/상태 | 현재 메모 |
|---|---|---|---|---|---|---|
| #19 | `[feat] 프로젝트룸 멤버 초대 API 추가` | `feature/project-room-members-invitations` | `develop` | `5cba6ce` | `build` pass, merge blocked | 6/25 기준 초대 취소, 멤버 역할 변경, 멤버 삭제/나가기 보정 완료 |
| #20 | `[feat] 채팅 기본 API 추가` | `feature/chat-basic-api` | `develop` | `d2eada3` | `build` pass, merge blocked | 6/25 기준 direct room 생성/기존 방 조회 API 보정 완료. read DTO는 최종 기준에서 추가 확인 필요 |
| #21 | `[feat] 작업 WBS 기본 API 추가` | `feature/work-task-wbs-api` | `develop` | `5f232da` | `build` pass, merge blocked | 6/25 기준 dashboard tasks, WBS board, WBS reorder 보정 완료. time-log API는 #30으로 분리 |
| #22 | `[feat] 일정 기본 API 추가` | `feature/schedule-basic-api` | `develop` | `3e2a7bf` | `build` pass, merge blocked | 일정 CRUD는 6/25 기본 API와 대체로 맞음. Google Calendar는 외부 캘린더 표시/동기화 범위로 별도 확인 |
| #23 | `[feat] 프로젝트룸 권한 검사 서비스 분리` | `feature/room-access-service` | `feature/schedule-basic-api` | `5aa677a` | checks 없음, merge clean | workflow 보강 전 stacked PR이라 GitHub check 없음. `room_members.status=ACTIVE`, `PROJECT_LEADER` 기준은 코드 재확인 완료 |
| #24 | `[chore] Google-only 인증 기반 정리` | `feature/auth-google-foundation` | `feature/room-access-service` | `15f9b7d` | checks 없음, merge clean, draft | 6/25 기준 Google authorize/callback endpoint 보정 완료. 실제 OAuth 검증은 501 TODO 유지 |
| #25 | `[feat] 자료 기본 저장 조회 API 추가` | `feature/resource-basic-foundation` | `feature/auth-google-foundation` | `36b9b55` | checks 없음, merge clean, draft | 6/25 기준 자료 메타데이터 수정/삭제, resource_comments, resource_versions, resource_summaries 조회 API, ResourceSummaryStatus, 삭제 정책 보정 완료 |
| #26 | `[feat] 에이전트 저장 기반 추가` | `feature/agent-storage-foundation` | `feature/resource-basic-foundation` | `1382c41` | checks 없음, merge clean, draft | 6/25 기준 agent enum 보정 완료. #25 base 병합 충돌 정리 완료 |
| #27 | `[chore] Entity Flyway 정합성 검사 추가` | `feature/entity-flyway-alignment` | `feature/agent-storage-foundation` | `66e2586` | checks 없음, merge clean, draft | agent 핵심 테이블 컬럼 집합/타입 검증 보강. `agent_model_call_logs` 정합성 확인 완료 |
| #28 | `[chore] stacked PR 테스트 검증 보강` | `feature/testcontainers-ci-foundation` | `feature/entity-flyway-alignment` | `810ec58` | `build` pass, merge clean, draft | stacked PR CI 보강 완료. #27 base 병합 뒤 CI 재통과 |
| #29 | `[chore] 2026-06-25 최신 기준 문서 반영` | `chore/latest-docs-2026-06-25` | `feature/testcontainers-ci-foundation` | latest pushed | `build` pass, merge clean, draft | 6/25 기준 문서와 워크플로 기준 반영, PR 재검토 상태 갱신 |
| #30 | `[feat] 타이머 작업시간 기본 API 추가` | `feature/time-log-basic-api` | `feature/work-task-wbs-api` | `f162377` | checks 없음, merge clean | 6/25 기준 time_logs start/pause/resume/stop/heartbeat 기본 API 추가 |
| #31 | `[feat] 자료 관련 문서 조회 API 추가` | `feature/resource-related-api` | `feature/resource-basic-foundation` | `4148e57` | checks 없음, merge clean, draft | 6/25 기준 resource_relations 조회 API 추가 |
| #32 | `[feat] 에이전트 작업 상태 조회 API 추가` | `feature/agent-job-status-api` | `feature/agent-storage-foundation` | `d667612` | checks 없음, merge clean, draft | 6/25 기준 agent_jobs 상태 조회 API 추가 |
| #33 | `[feat] 에이전트 제안함 조회 API 추가` | `feature/agent-suggestion-list-api` | `feature/agent-job-status-api` | `a863439` | checks 없음, merge clean, draft | 6/25 기준 agent_suggestions 개인/프로젝트룸 조회 API 추가 |
| #34 | `[feat] 에이전트 작업 이벤트 조회 API 추가` | `feature/agent-job-events-api` | `feature/agent-suggestion-list-api` | `698557a` | checks 없음, merge clean, draft | 6/25 기준 agent_job_events 조회 API 추가 |
| #35 | `[feat] 에이전트 제안 상태 수정 API 추가` | `feature/agent-suggestion-update-api` | `feature/agent-job-events-api` | `f27b0ce` | checks 없음, merge clean, draft | 6/25 기준 agent_suggestions 상태/내용 수정 API 추가. 확정 업무 데이터 생성은 하지 않음 |
| #36 | `[feat] 자료 AI 문서 조회 API 추가` | `feature/resource-ai-document-api` | `feature/agent-suggestion-update-api` | `d8bea2b` | checks 없음, merge clean, draft | 6/25 기준 resource ai-document 조회 API 추가 |
| #37 | `[feat] 프로젝트룸 AI 문서 목록 조회 API 추가` | `feature/room-ai-documents-api` | `feature/resource-ai-document-api` | `aa52ec1` | checks 없음, merge clean, draft | 6/25 기준 project-room ai-documents 목록 조회 API 추가 |
| #38 | `[test] 엔티티 경계 가드 추가` | `chore/entity-boundary-guards` | `feature/testcontainers-ci-foundation` | `a22ee45` | `build` pass, merge clean, draft | BaseTimeEntity, global/entity Java source, local_* JPA entity 금지 테스트 추가 |
| #39 | `[feat] 저장 용량 조회 API 추가` | `feature/storage-usage-api` | `feature/resource-related-api` | `282a7d1` | checks 없음, merge clean, draft | 6/25 기준 storage usage 조회 API와 accounting boundary 추가 |
| #40 | `[feat] 자료 분석 작업 생성 API 추가` | `feature/analyze-resource-job-api` | `feature/room-ai-documents-api` | `769a707` | checks 없음, merge clean, draft | 6/25 기준 analyze-resource agent job 생성 API 추가 |
| #41 | `[feat] 요구사항 후보 생성 작업 API 추가` | `feature/generate-requirements-job-api` | `feature/analyze-resource-job-api` | `9c12eae` | checks 없음, merge clean, draft | 6/25 기준 generate-requirements agent job 생성 API 추가 |
| #42 | `[feat] TODO 후보 생성 작업 API 추가` | `feature/generate-tasks-job-api` | `feature/generate-requirements-job-api` | `7a25da0` | checks 없음, merge clean, draft | 6/25 기준 generate-tasks agent job 생성 API 추가 |
| #43 | `[feat] WBS 후보 생성 작업 API 추가` | `feature/generate-wbs-job-api` | `feature/generate-tasks-job-api` | `7429dec` | checks 없음, merge clean, draft | 6/25 기준 generate-wbs agent job 생성 API 추가 |
| #44 | `[feat] 확인 질문 후보 생성 작업 API 추가` | `feature/generate-questions-job-api` | `feature/generate-wbs-job-api` | `a8eea88` | checks 없음, merge clean, draft | 6/25 기준 generate-questions agent job 생성 API 추가 |
| #45 | `[feat] 계약서 문서 검토 작업 API 추가` | `feature/review-contract-documents-job-api` | `feature/generate-questions-job-api` | `8fefcee` | checks 없음, merge clean, draft | 6/25 기준 review-contract-documents agent job 생성 API 추가 |
| #53 | `[feat] 에이전트 작업 dispatch 경계 추가` | `feature/agent-job-dispatch-boundary` | `feature/review-contract-documents-job-api` | `8059952` | checks 없음, merge clean, draft | AgentJob 생성 후 AFTER_COMMIT dispatch port, in-memory queue adapter 초안, enqueue 실패 기록 추가 |
| #46 | `[feat] 자료 다운로드 URL API 뼈대 추가` | `feature/resource-download-url-api` | `feature/resource-related-api` | `5e70334` | checks 없음, merge clean, draft | 6/25 기준 resource download-url API와 StorageDownloadUrlProvider 경계 추가 |
| #52 | `[feat] S3 다운로드 URL Provider 추가` | `feature/s3-download-url-provider` | `feature/resource-download-url-api` | `0de5a0a` | checks 없음, merge clean, draft | #46 provider 경계에 S3 presigned download URL 구현 추가 |
| #54 | `[feat] S3 저장 서비스 경계 추가` | `feature/s3-storage-service-boundary` | `feature/s3-download-url-provider` | `ae6eed7` | checks 없음, merge clean, draft | #52 S3 설정 위에 StorageService 저장/삭제 경계 추가. 업로드 API endpoint는 후속 PR |
| #55 | `[feat] 자료 multipart 업로드 API 연결` | `feature/resource-multipart-upload-api` | `feature/s3-storage-service-boundary` | `c186557` | checks 없음, merge clean, draft | #54 StorageService를 사용해 resource/file/version 생성까지 연결 |
| #56 | `[feat] 자료 업로드 실패 보상 삭제 추가` | `feature/resource-upload-compensation` | `feature/resource-multipart-upload-api` | `b18efb6` | checks 없음, merge clean, draft | #55 업로드 흐름에서 DB 저장 실패 시 StorageService.delete 보상 처리 |
| #57 | `[feat] 자료 업로드 정책 검사 추가` | `feature/resource-upload-policy` | `feature/resource-upload-compensation` | `756174e` | checks 없음, merge clean, draft | #56 업로드 흐름에 설정 기반 크기/MIME 정책 검사 추가 |
| #47 | `[feat] 내 프로필 수정 API 추가` | `feature/user-me-update-api` | `develop` | `2a132a6` | `build` pass, merge blocked, draft | 6/25 기준 PATCH /api/me 추가 |
| #48 | `[feat] 사용자 설정 API 추가` | `feature/user-preferences-api` | `feature/user-me-update-api` | `bbad8ae` | checks 없음, merge clean, draft | 6/25 기준 GET/PATCH /api/me/preferences 추가 |
| #49 | `[feat] 사용자 알림 설정 API 추가` | `feature/user-notification-preferences-api` | `feature/user-preferences-api` | `f94239e` | checks 없음, merge clean, draft | 6/25 기준 GET/PATCH /api/me/notification-preferences 추가 |
| #50 | `[feat] 사용자 개인정보 동의 API 추가` | `feature/user-privacy-consents-api` | `feature/user-notification-preferences-api` | `bb44f55` | checks 없음, merge clean, draft | 6/25 기준 GET/PATCH /api/me/privacy-consents 추가 |
| #51 | `[feat] 내 프로젝트룸 목록 API 추가` | `feature/user-project-rooms-api` | `feature/user-privacy-consents-api` | `8650257` | checks 없음, merge clean, draft | 6/25 기준 GET /api/me/project-rooms 추가 |

## Draft PR 후속 전환 메모

2026-06-25 06:29 KST 기준 draft PR은 #24, #25, #26, #27, #28, #29, #31, #32, #33, #34, #35, #36, #37, #38, #39, #40, #41, #42, #43, #44, #45, #46, #47, #48, #49, #50, #51, #52, #53, #54, #55, #56, #57다.
#19, #20, #21, #22, #23, #30은 ready 상태다.

draft PR은 폐기 상태가 아니다.
stacked base가 정리되고 각 PR의 로컬 검증과 GitHub Actions CI 상태를 확인한 뒤 ready PR로 전환한다.
특히 #24~#29와 #31~#57은 앞선 base PR merge 순서에 영향을 받으므로, 지금 바로 ready로 바꾸지 않고 handoff에 추적한다.

## 6/25 기준 재검토 후보

| 영역 | 새 기준 | 현재 할 일 |
|---|---|---|
| 문서 기준 | `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`, `Bubli_백엔드_개발_가이드_2026-06-25.md` | 6/24 참조를 활성 문서에서 제거하고 archive로만 보존 |
| 프로젝트룸/초대 | `POST/GET /api/project-rooms/{roomId}/invitations`, `PATCH /api/invitations/{id}/accept`, `PATCH /api/invitations/{id}/cancel`, 멤버 역할 변경/삭제 | #19에서 보정 완료. 6/25 기준에는 invite-links API가 없다 |
| 채팅 | room sequence, 읽음 상태, direct room API 기준 | #20에서 `POST /api/chat/direct-rooms` 보정 완료. `lastReadSequence`와 read DTO는 최종 기준에서 추가 확인 필요 |
| 작업/WBS/타이머 | WBS, tasks, time_logs 책임 분리 | #21에서 dashboard tasks, WBS board/reorder 보정 완료. #30에서 time_logs 기본 API 추가 |
| 일정 | personal/room 일정, Google Calendar 범위 | #22 일정 CRUD는 기본선으로 둔다. Google Calendar 직접 쓰기는 섞지 않고 `google_event_id`/sync 상태만 별도 검토한다 |
| 인증 | Google-only auth, `GET /api/auth/google/authorize`, `POST /api/auth/google/callback`, refresh/logout | #24에서 endpoint surface와 `.http` 예시 보정 완료. 실제 OAuth 연동은 후속 구현 |
| 사용자 | `GET /api/me`, `PATCH /api/me`, 사용자별 설정 API | #47에서 `PATCH /api/me` 보정 완료. #48에서 `GET/PATCH /api/me/preferences` 보정 완료. #49에서 `GET/PATCH /api/me/notification-preferences` 보정 완료. #50에서 `GET/PATCH /api/me/privacy-consents` 보정 완료. #51에서 `GET /api/me/project-rooms` 보정 완료 |
| 자료 | `resources`, `resource_files`, `resource_versions`, `resource_comments`, `resource_summaries`, `resource_relations`, `ai_documents` | #25에서 metadata patch/delete, resource_comments, resource_versions, resource_summaries 조회 API, ResourceSummaryStatus, 삭제 정책 보정 완료. #31에서 resource_relations 조회 API 추가. #46에서 download-url API 뼈대와 Provider 경계 추가. #52에서 S3 presigned download URL Provider 추가. #54에서 S3 저장/삭제 StorageService 경계 추가. #55에서 multipart upload와 resource_files/resource_versions v1 생성 연결. #56에서 업로드 후 DB 저장 실패 시 보상 삭제 추가. #57에서 설정 기반 크기/MIME 정책 검사 추가 |
| 에이전트 | 후보는 `agent_suggestions`, AI 문서는 `ai_documents`, 확정 저장은 각 도메인 Service | #26에서 enum을 6/25 후보 타입과 agent job 흐름에 맞게 확장 완료. #32~#37에서 job 상태, suggestion 목록/수정, job event, resource/project-room ai-document 조회 API 추가. #40~#45에서 job 생성 API 추가. #53에서 dispatch port 경계, in-memory queue adapter 초안, enqueue 실패 기록 추가 |
| Tauri SQLite | `local_*`는 서버 JPA 엔티티가 아님 | 서버 코드에 local table 엔티티가 생기지 않았는지 확인 |

## 6/24 기준 메모 보존

아래 내용은 6/24 기준으로 남긴 이전 재검토 메모다.
새 작업은 위의 `6/25 기준 재검토 후보`를 우선한다.

## API Design 기준 재검토 후보

| 영역 | API Design 기준 | 현재 판단 |
|---|---|---|
| 프로젝트룸 초대 | 6/25 기준은 가입 사용자 ID 초대, 수락, 취소, 멤버 역할 변경/삭제 중심 | #19 보정 완료. 6/24 메모의 invite-links API는 6/25 기준에서 제외됨 |
| 인증 | Google-only authorize/callback, refresh, logout | #24 보정 완료. signup/email-password는 되살리지 않음 |
| 사용자 프로필 | `GET /api/me`, `PATCH /api/me` 포함 | #47 보정 완료. 로그인 사용자 기준 프로필 조회/수정을 DTO로 제공한다 |
| 사용자 설정 | `GET/PATCH /api/me/preferences`, `GET/PATCH /api/me/notification-preferences`, `GET/PATCH /api/me/privacy-consents` 포함 | #48 preferences 보정 완료. #49 notification-preferences 보정 완료. #50 privacy-consents 보정 완료. 기본 홈, 기본 프로젝트룸, 알림 종류별 On/Off, 민감 기능 동의 상태를 조회/수정한다 |
| 자료 상태값 | `ResourceResponse.status` 예시는 `UPLOADED`, `ANALYZING`, `ANALYZED`, `FAILED`, `ARCHIVED` | 6/25 데이터 딕셔너리 기준으로 `ResourceSummaryStatus`와 `ResourceStatus.DELETED` 제거는 #25에서 보정했다. `ResourceResponse.status` 예시의 `UPLOADED/ARCHIVED`와 코드 `UPLOADING/READY` 명칭 차이는 최종 API 기준에서 추가 확인 필요 |
| 자료 업로드 | `POST /api/resources`는 개인 또는 프로젝트룸 자료 업로드 | #25는 파일/S3 업로드 전 단계의 자료 카드 메타데이터 저장/조회 기반, 댓글 API, 파일 메타데이터/버전 API, summary 조회 API까지 구현함. #46에서 download-url API 뼈대를 추가했고, #52에서 S3 presigned URL Provider를 추가함. #54에서 S3 저장/삭제 Service 경계를 추가했고, #55에서 multipart upload를 `resources`, `resource_files`, `resource_versions` v1 생성까지 연결함. #56에서 DB 저장 실패 시 저장소 객체 보상 삭제를 추가했고, #57에서 파일 크기/MIME allow-list 검사를 추가함. #39와 #57은 현재 형제 stack이어서 storage usage accounting 호출은 stack 정렬 뒤 별도 보정 필요 |
| 자료 수정/삭제 | `PATCH /api/resources/{id}`, `DELETE /api/resources/{id}` 포함 | #25 보정 완료. #24 base 병합 충돌도 해결되어 PR mergeState는 CLEAN |
| 자료 댓글 | `GET/POST /api/resources/{id}/comments`, `PATCH/DELETE /api/resource-comments/{id}` 포함 | #25 보정 완료. 작성자만 수정/삭제 가능하고 삭제는 `deleted_at` 처리 |
| 자료 버전 | `GET/POST /api/resources/{id}/versions` 포함 | #25 보정 완료. `resource_files`와 `resource_versions` 메타데이터를 저장/조회한다 |
| 자료 요약 | `GET /api/resources/{id}/summary` 포함 | #25 보정 완료. `resource_summaries` 최신 행을 DTO로 반환한다 |
| 자료 관련 문서 | `GET /api/resources/{id}/related` 포함 | #31 보정 완료. `resource_relations` 목록과 접근 가능한 관련 자료 DTO를 반환한다 |
| 에이전트 후보 타입 | 기획/가이드는 TODO, WBS, REQUIREMENT, SCHEDULE, QUESTION, CONTRACT_FIELD, CONTRACT_REVIEW, DOCUMENT_DRAFT, DAILY_SUMMARY, MEMO 등 후보를 통합 저장한다고 설명 | #26 보정 완료. `TASK`, `REVIEW_ITEM`은 기존 데이터 모델 표와 저장값 호환을 위해 유지 |
| 에이전트 작업 조회 | `GET /api/agent-jobs/{jobId}`, `GET /api/agent-jobs/{jobId}/events` 포함 | #32, #34 보정 완료. 본인 job만 조회하고 event 목록은 `agent_job_events` 기준으로 반환한다 |
| 에이전트 후보 수정 | `PATCH /api/agent/suggestions/{id}`는 후보 승인, 수정, 보류, 삭제를 다룸 | #35 보정 완료. `agent_suggestions` 자체의 상태/내용만 수정하며, 승인 후 `tasks`, `wbs_items`, `schedules`, `memos` 확정 저장은 후속 target domain Service PR에서 다룬다 |
| AI 문서 조회 | `GET /api/resources/{id}/ai-document`는 자료의 AI 문서 분류와 분석 상태를 반환 | #36 보정 완료. 자료 읽기 권한 확인 후 `ai_documents` 단건 상태를 반환한다 |
| AI 문서 목록 | `GET /api/project-rooms/{roomId}/ai-documents`는 프로젝트룸 AI 문서 분석 목록을 반환 | #37 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `ai_documents` 목록을 반환한다 |
| 엔티티 경계 | BaseTimeEntity, global 공통 엔티티, Tauri `local_*` 서버 엔티티 금지 | #38 보정 완료. 금지 구조를 테스트로 고정했다 |
| 저장 용량 | `GET /api/storage/usage`는 사용자별 서버 저장 용량과 남은 용량 조회 | #39 보정 완료. 개인/참여 룸 usage row 조회와 합계 계산을 추가했고, 업로드 사용량 기록/해제와 quota 초과 검사 Service 경계를 추가했다 |
| 에이전트 작업 생성 | `POST /api/ai/analyze-resource`는 자료 요약, 임베딩, AI 문서 분류 작업 생성 | #40 보정 완료. 자료 접근 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 작업 생성 | `POST /api/ai/generate-requirements`는 요구사항 후보 생성 작업 생성 | #41 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 작업 생성 | `POST /api/ai/generate-tasks`는 TODO 후보 생성 작업 생성 | #42 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 작업 생성 | `POST /api/ai/generate-wbs`는 WBS 후보 생성 작업 생성 | #43 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 작업 생성 | `POST /api/ai/generate-questions`는 확인 질문 후보 생성 작업 생성 | #44 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 작업 생성 | `POST /api/ai/review-contract-documents`는 계약서와 요구사항 문서 검토 작업 생성 | #45 보정 완료. 프로젝트룸 ACTIVE 멤버 권한 확인 후 `agent_jobs` PENDING job 생성까지만 처리한다 |
| 에이전트 제안함 | `GET /api/agent/suggestions`, `GET /api/project-rooms/{roomId}/agent/suggestions` 포함 | #33 보정 완료. 개인 제안함과 프로젝트룸 ACTIVE 멤버 제안함 조회를 제공한다 |
| Entity/Flyway | `agent_model_call_logs` 엔티티와 Flyway 테이블 정의 | #27 보강 완료. `agent_model_call_logs` Flyway 정의는 6/25 데이터 모델/Entity와 맞으며 agent 핵심 테이블 컬럼 집합과 주요 타입을 테스트로 고정했다 |
| 채팅 | `POST /api/chat/direct-rooms` 포함 | #20 보정 완료. 기존 DIRECT 방이 있으면 재사용하고 없으면 새 방을 만든다 |
| 작업 대시보드 | `GET /api/dashboard/tasks` 포함 | #21 보정 완료. 개인 TODO와 담당 프로젝트룸 TODO를 함께 조회한다 |
| WBS 작업판 | `GET /api/project-rooms/{roomId}/wbs-board` 포함 | #21 보정 완료. WBS 항목과 프로젝트룸 TODO를 함께 반환한다 |
| 타이머 | `POST /api/time-logs/start`, pause, resume, stop, heartbeat 포함 | #30 보정 완료. `personal.timer`에서 `time_logs`를 원본으로 처리한다 |
| 일정 | `GET/POST/PATCH/DELETE /api/schedules` | #22와 대체로 맞음. Google Calendar 직접 연동은 별도 PR로 분리 가능 |

## 구조 검토 메모

- 현재 코드에는 `personal/*`, `work/*` 하위 도메인 패키지가 들어와 있다. 이 방향은 팀이 말한 패키지 구조와 맞다.
- `global`은 공통 설정, 응답, 에러, 보안, 검증 코드만 두는 기준으로 유지한다.
- `global/entity`나 공통 `BaseTimeEntity`는 만들지 않는다.
- `createdAt`, `updatedAt`은 각 엔티티 필드로 직접 둔다.
- `local_*` Tauri SQLite 테이블은 서버 JPA 엔티티로 만들지 않는다.

## 다음 작업 우선순위

1. #23은 #22 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
2. #24는 #22, #23 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
3. #25는 #22, #23, #24 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
4. #26은 #25 최신 base 병합 후 mergeState `CLEAN`으로 정리됐다.
5. #27은 #26 최신 base 병합 후 mergeState `CLEAN`으로 정리됐다.
6. #28은 #27 최신 base 병합 뒤 GitHub Actions CI `build`가 통과했다.
7. #29는 #28 최신 base 병합 뒤 GitHub Actions CI를 다시 확인한다.
8. draft PR #24~#29, #31~#57은 앞선 base PR merge와 검증 상태가 정리되면 ready PR로 전환한다.
9. 다음 추천 작업은 resource upload/storage usage stack 정렬 뒤 파일 삭제와 용량 해제 연결, agent dispatch retry/outbox 초안, 또는 Entity/Flyway FK/인덱스/enum 값 검증 보강이다.
10. #19~#57은 6/25 기준으로 계속 재검토하고 차이만 보정한다.

## 6/25 기준 가능한 작업

| 우선순위 | 작업 | 메모 |
|---|---|---|
| 1 | 패키지 구조와 도메인 위치 점검 | `personal/*`, `work/*`, `project`, `resource`, `agent` 등 기존 구조 기준 |
| 2 | Entity, Enum, Repository, Flyway 기준 점검 | API보다 데이터 딕셔너리를 우선한다 |
| 3 | Security/JWT/CurrentUser 기반 점검 | 모든 API에서 재사용되는 공통 기반 |
| 4 | 공통 응답, 공통 에러, Validation 점검 | API 확정 뒤 Controller에 붙일 기반 |
| 5 | 프로젝트룸 권한 검사 서비스 점검 | `room_members.status=ACTIVE`, `PROJECT_LEADER` 권한 확인 |
| 6 | Testcontainers와 테스트 support 점검 | API 확정 뒤 PR별 테스트를 빠르게 붙이기 위함 |
| 7 | `.http` 파일 구조 정리 | 6/25 API 기준 요청 예시를 맞춘다 |

상세 작업 지시와 복붙 프롬프트는 `docs/CURRENT_API_BASELINE_WORK.md`를 따른다.

## 작업 시 주의할 점

| 주의할 점 | 이유 |
|---|---|
| 여러 기능을 한 브랜치에 섞지 않기 | 기준 문서가 바뀌면 보정하기 어렵다 |
| 현재 API를 최종 확정이라고 쓰지 않기 | 지금은 작업 기준선이다 |
| 기획/DB와 충돌하는 API는 기록하기 | 현재 API만 보고 무리하게 확정하지 않는다 |
| Agent/RAG payload 과확정 금지 | 에이전트 흐름은 변경 가능성이 높다 |
| WebSocket payload 과확정 금지 | 채팅/이벤트 범위 변경 가능성이 있다 |

## 새 기준 문서 수신 절차

사용자가 새 API 명세서나 DB 문서를 주면서 완성본이라고 말하면 아래를 수행한다.

1. 새 기준 파일 위치를 확인한다.
2. 기존 6/25 기준 문서와 비교한다.
3. 새 파일이 기준이면 기준 문서 지도와 스킬의 API 기준 경로를 갱신한다.
4. 이 문서의 `현재 작업 모드`를 새 API 기준 작업 모드로 바꾼다.
5. 재검토 후보를 새 기준으로 다시 작성한다.
6. 현재 구현과 새 API의 endpoint, DTO, 테스트 차이를 다시 정리한다.
7. 이후 한 PR씩 차이 보정 작업을 하고 로컬 검증과 GitHub Actions CI를 확인한다.

## 작업 완료 기준

문서만 수정한 경우:

- `ls docs/00_BACKEND_START_HERE.md docs/WORK_HANDOFF.md docs/CODEX_BACKEND_WORKFLOW.md`
- `rg "09_Data-Model.md|09C_DB-Tauri-SQLite.md|10_API-Design.md|WORK_HANDOFF|GitHub Actions|CI" docs --glob '!docs/archive/**'`
- 예전 다운로드 폴더의 API Design 절대경로가 문서나 코드에 남아 있지 않은지 확인한다.
- `git diff --check`

코드를 수정한 경우:

- `./gradlew compileTestJava`
- `./gradlew cleanTest test`
- `git diff --check`
- GitHub Actions CI 통과 확인

## 갱신 규칙

- PR을 새로 만들거나 수정하면 이 문서의 열린 PR 상태를 갱신한다.
- 기준 문서와 맞지 않는 부분을 발견하면 `API Design 기준 재검토 후보`에 추가한다.
- API 명세 완성본이 들어오면 `현재 작업 모드`, API 기준 경로, PR 재검토 후보를 갱신한다.
- 작업 완료 후 마지막 확인 시각을 갱신한다.
- CI가 아직 확인되지 않았으면 완료로 쓰지 않는다.
