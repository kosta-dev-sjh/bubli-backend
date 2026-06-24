# Bubli Backend Work Handoff

Last checked: 2026-06-25 01:51 KST

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
- 열린 PR #19~#29 상태 재확인 완료 (2026-06-25 01:38 KST)
- 엔티티 44개, Repository 4개, Controller 4개, Service 5개 확인
- 6/25 기준 세부 작업 지시는 `docs/CURRENT_API_BASELINE_WORK.md`를 기준으로 나눈다.

## 최근 완료 작업

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
- #27: head `9a8827a`, base `feature/agent-storage-foundation`, mergeState `CLEAN`, GitHub checks 없음
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
- Flyway의 `agent_model_call_logs` 테이블 정의와 `AgentModelCallLog` 엔티티가 맞지 않는 흔적이 있어 Entity/Flyway 정합성 카드에서 확인해야 한다.

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
| #21 | `[feat] 작업 WBS 기본 API 추가` | `feature/work-task-wbs-api` | `develop` | `5f232da` | `build` pass, merge blocked | 6/25 기준 dashboard tasks, WBS board, WBS reorder 보정 완료. time-log API는 별도 보정 필요 |
| #22 | `[feat] 일정 기본 API 추가` | `feature/schedule-basic-api` | `develop` | `3e2a7bf` | `build` pass, merge blocked | 일정 CRUD는 6/25 기본 API와 대체로 맞음. Google Calendar는 외부 캘린더 표시/동기화 범위로 별도 확인 |
| #23 | `[feat] 프로젝트룸 권한 검사 서비스 분리` | `feature/room-access-service` | `feature/schedule-basic-api` | `5aa677a` | checks 없음, merge clean | workflow 보강 전 stacked PR이라 GitHub check 없음. `room_members.status=ACTIVE`, `PROJECT_LEADER` 기준은 코드 재확인 완료 |
| #24 | `[chore] Google-only 인증 기반 정리` | `feature/auth-google-foundation` | `feature/room-access-service` | `15f9b7d` | checks 없음, merge clean | 6/25 기준 Google authorize/callback endpoint 보정 완료. 실제 OAuth 검증은 501 TODO 유지 |
| #25 | `[feat] 자료 기본 저장 조회 API 추가` | `feature/resource-basic-foundation` | `feature/auth-google-foundation` | `14c522d` | checks 없음, merge clean | 6/25 기준 자료 메타데이터 수정/삭제 보정 완료. #24 base 병합 충돌 정리 완료 |
| #26 | `[feat] 에이전트 저장 기반 추가` | `feature/agent-storage-foundation` | `feature/resource-basic-foundation` | `1382c41` | checks 없음, merge clean | 6/25 기준 agent enum 보정 완료. #25 base 병합 충돌 정리 완료 |
| #27 | `[chore] Entity Flyway 정합성 검사 추가` | `feature/entity-flyway-alignment` | `feature/agent-storage-foundation` | `9a8827a` | checks 없음, merge clean | 테이블/컬럼 검사 유지. #26 base 병합 충돌 정리 완료 |
| #28 | `[chore] stacked PR 테스트 검증 보강` | `feature/testcontainers-ci-foundation` | `feature/entity-flyway-alignment` | `810ec58` | `build` pass, merge clean | stacked PR CI 보강 완료. #27 base 병합 뒤 CI 재통과 |
| #29 | `[chore] 2026-06-25 최신 기준 문서 반영` | `chore/latest-docs-2026-06-25` | `feature/testcontainers-ci-foundation` | `903165e` | `build` pass, merge clean | 6/25 기준 문서와 워크플로 기준 반영, PR 재검토 상태 갱신 |

## 6/25 기준 재검토 후보

| 영역 | 새 기준 | 현재 할 일 |
|---|---|---|
| 문서 기준 | `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`, `Bubli_백엔드_개발_가이드_2026-06-25.md` | 6/24 참조를 활성 문서에서 제거하고 archive로만 보존 |
| 프로젝트룸/초대 | `POST/GET /api/project-rooms/{roomId}/invitations`, `PATCH /api/invitations/{id}/accept`, `PATCH /api/invitations/{id}/cancel`, 멤버 역할 변경/삭제 | #19에서 보정 완료. 6/25 기준에는 invite-links API가 없다 |
| 채팅 | room sequence, 읽음 상태, direct room API 기준 | #20에서 `POST /api/chat/direct-rooms` 보정 완료. `lastReadSequence`와 read DTO는 최종 기준에서 추가 확인 필요 |
| 작업/WBS/타이머 | WBS, tasks, time_logs 책임 분리 | #21에서 dashboard tasks, WBS board/reorder 보정 완료. time_logs는 별도 PR로 나눈다 |
| 일정 | personal/room 일정, Google Calendar 범위 | #22 일정 CRUD는 기본선으로 둔다. Google Calendar 직접 쓰기는 섞지 않고 `google_event_id`/sync 상태만 별도 검토한다 |
| 인증 | Google-only auth, `GET /api/auth/google/authorize`, `POST /api/auth/google/callback`, refresh/logout | #24에서 endpoint surface와 `.http` 예시 보정 완료. 실제 OAuth 연동은 후속 구현 |
| 자료 | `resources`, `resource_files`, `resource_versions`, `resource_comments`, `resource_summaries`, `ai_documents` | #25에서 metadata patch/delete 보정 완료. download-url/version/comment/summary/ai-document API는 후속 PR로 나눈다 |
| 에이전트 | 후보는 `agent_suggestions`, AI 문서는 `ai_documents`, 확정 저장은 각 도메인 Service | #26에서 enum을 6/25 후보 타입과 agent job 흐름에 맞게 확장 완료 |
| Tauri SQLite | `local_*`는 서버 JPA 엔티티가 아님 | 서버 코드에 local table 엔티티가 생기지 않았는지 확인 |

## 6/24 기준 메모 보존

아래 내용은 6/24 기준으로 남긴 이전 재검토 메모다.
새 작업은 위의 `6/25 기준 재검토 후보`를 우선한다.

## API Design 기준 재검토 후보

| 영역 | API Design 기준 | 현재 판단 |
|---|---|---|
| 프로젝트룸 초대 | 6/25 기준은 가입 사용자 ID 초대, 수락, 취소, 멤버 역할 변경/삭제 중심 | #19 보정 완료. 6/24 메모의 invite-links API는 6/25 기준에서 제외됨 |
| 인증 | Google-only authorize/callback, refresh, logout | #24 보정 완료. signup/email-password는 되살리지 않음 |
| 자료 상태값 | `ResourceResponse.status` 예시는 `UPLOADED`, `ANALYZING`, `ANALYZED`, `FAILED`, `ARCHIVED` | 당시 데이터 딕셔너리와 코드 enum은 `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED`이었다. 6/25 기준으로 상태값 명칭 재확인 필요 |
| 자료 업로드 | `POST /api/resources`는 개인 또는 프로젝트룸 자료 업로드 | #25는 파일/S3 업로드 전 단계의 자료 카드 메타데이터 저장/조회 기반만 구현함. multipart 업로드, 파일 메타데이터, 버전 생성은 별도 PR 필요 |
| 자료 수정/삭제 | `PATCH /api/resources/{id}`, `DELETE /api/resources/{id}` 포함 | #25 보정 완료. #24 base 병합 충돌도 해결되어 PR mergeState는 CLEAN |
| 에이전트 후보 타입 | 기획/가이드는 TODO, WBS, REQUIREMENT, SCHEDULE, QUESTION, CONTRACT_FIELD, CONTRACT_REVIEW, DOCUMENT_DRAFT, DAILY_SUMMARY, MEMO 등 후보를 통합 저장한다고 설명 | #26 보정 완료. `TASK`, `REVIEW_ITEM`은 기존 데이터 모델 표와 저장값 호환을 위해 유지 |
| Entity/Flyway | `agent_model_call_logs` 엔티티와 Flyway 테이블 정의 | Flyway 정의가 모델 호출 로그가 아니라 agent suggestion 형태 컬럼을 가진 것으로 보인다. 별도 정합성 PR에서 확인 필요 |
| 채팅 | `POST /api/chat/direct-rooms` 포함 | #20 보정 완료. 기존 DIRECT 방이 있으면 재사용하고 없으면 새 방을 만든다 |
| 작업 대시보드 | `GET /api/dashboard/tasks` 포함 | #21 보정 완료. 개인 TODO와 담당 프로젝트룸 TODO를 함께 조회한다 |
| WBS 작업판 | `GET /api/project-rooms/{roomId}/wbs-board` 포함 | #21 보정 완료. WBS 항목과 프로젝트룸 TODO를 함께 반환한다 |
| 타이머 | `POST /api/time-logs/start`, pause, resume, stop, heartbeat 포함 | #21에 섞지 말고 `personal/timer` 또는 별도 time-log PR로 분리하는 편이 안전함 |
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
8. 다음 추천 작업은 `time_logs`, resource download/version/comment/summary/ai-document API 중 하나를 별도 PR로 나누는 것이다.
9. #19~#29는 6/25 기준으로 계속 재검토하고 차이만 보정한다.

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
