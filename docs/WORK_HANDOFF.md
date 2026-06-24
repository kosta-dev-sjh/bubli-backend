# Bubli Backend Work Handoff

Last checked: 2026-06-25 03:37 KST

이 문서는 백엔드 현재 상태를 이어받기 위한 인수인계 문서다.
작업이 끝날 때마다 이 문서의 PR 상태, 확인 결과, 다음 작업을 갱신한다.

## 현재 기준

| 항목 | 값 |
|---|---|
| 로컬 레포 | `/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend` |
| 현재 확인 브랜치 | `chore/entity-boundary-guards` |
| 원격 기준 브랜치 | `develop` |
| 시작 문서 | `docs/00_BACKEND_START_HERE.md` |
| API 기준 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md` |
| DB 기준 | `/Users/maren/EDU/Final Project/04_개발_작업공간/DB_팀검토_2026-06-23/Bubli_DB_검토보드/09_데이터딕셔너리_회의반영_2026-06-24.html` |
| 백엔드 규칙 | `docs/Bubli_백엔드_개발_가이드_2026-06-24.md` |
| 현재 API 기준 작업 계획 | `docs/CURRENT_API_BASELINE_WORK.md` |
| API 확정 전 기반 작업 카드 | `docs/API_PRE_FINAL_TASKS.md` |

## 현재 작업 모드

상태: 현재 `10_API-Design.md`를 작업 기준선으로 삼아 구현한다.

지금은 API 수정본을 기다리며 멈추지 않는다.
현재 API 기준으로 백엔드 뼈대와 기본 API를 만들고, 사용자가 새 API 명세 완성본을 주면 `API 명세 완성본 수신 절차`로 전환해 차이 보정 작업을 한다.

현재 자동화 목표에서는 #22 `feature/schedule-basic-api`를 구현 기준선으로 인정한다.
따라서 `develop`에 아직 없는 `ScheduleService`가 있어도 멈추지 않고, #22 위에 이어지는 foundation 작업은 stacked PR 또는 현재 브랜치 기준 커밋으로 정리한다.
stacked PR이라 GitHub Actions가 실행되지 않으면 로컬 검증 결과를 남기고, #22 merge 후 `develop` 기준 CI를 다시 확인한다.

현재 컴파일 기준:

- `./gradlew compileTestJava` 통과 (2026-06-24 21:44 KST)
- `./gradlew cleanTest test` 통과 (2026-06-24 21:44 KST)
- `git diff --check` 통과 (2026-06-24 21:44 KST)
- `./gradlew compileTestJava` 통과 (2026-06-25 03:34 KST, entity boundary guard)
- `./gradlew cleanTest test` 통과 (2026-06-25 03:34 KST, entity boundary guard)
- `git diff --check` 통과 (2026-06-25 03:34 KST, entity boundary guard)
- GitHub Actions CI 통과 (#28, 2026-06-24 21:47 KST)
- 엔티티 44개, Repository 4개, Controller 4개, Service 5개 확인
- 현재 API 기준 세부 작업 지시는 `docs/CURRENT_API_BASELINE_WORK.md`를 기준으로 나눈다.

## 최근 완료 작업

### 작업 카드 9-1. 엔티티 경계 가드 테스트

처리 시각: 2026-06-25 03:34 KST

변경 내용:

- `chore/entity-boundary-guards`를 #28 `feature/testcontainers-ci-foundation` 위의 새 브랜치로 만들었다.
- `EntityBoundaryGuardTest`를 추가했다.
- 서버 코드에 `BaseTimeEntity.java`가 생기면 테스트가 실패하도록 했다.
- `global/entity` 아래 Java 소스가 생기면 테스트가 실패하도록 했다.
- 서버 JPA Entity가 Tauri 로컬 SQLite용 `local_*` 테이블에 매핑되면 테스트가 실패하도록 했다.
- 빈 로컬 디렉터리는 Git 추적 대상이 아니라 위반으로 보지 않고, 실제 Java 소스만 검사한다.

검증 결과:

- 최초 `./gradlew cleanTest test`는 빈 `global/entity` 디렉터리를 위반으로 오탐해 1회 실패했다.
- 테스트 기준을 실제 Java 소스 기준으로 보정했다.
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions CI 통과: #38 `build` pass, 1m17s

메모:

- 이번 PR은 금지된 엔티티 경계를 테스트로 고정하는 작업만 다룬다.
- 애플리케이션 코드, Entity, Flyway, Gradle, PR 템플릿, README, SETUP은 건드리지 않았다.
- `global/config`, `global/error`, `global/response`, `global/security` 같은 공통 코드 패키지는 허용한다.

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

- 현재 `10_API-Design.md`를 기준으로 자료 기본 저장/조회 API 뼈대를 추가했다.
- `ResourceController`, `ResourceService`, `ResourceRepository`를 추가했다.
- `GET /api/resources?scope=personal`, `GET /api/project-rooms/{roomId}/resources`, `POST /api/resources`, `GET /api/resources/{resourceId}`를 구현했다.
- API 응답은 Entity를 직접 반환하지 않고 `ResourceResponse`와 `ResourceResult`로 분리했다.
- 개인 자료는 `owner_id`, 프로젝트룸 자료는 `room_id`와 ACTIVE `room_members` 기준으로 접근을 확인한다.
- `docs/http/resource.http`에 현재 API 기준 수동 검증 예시를 추가했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 자료 카드 메타데이터 저장/조회 기반이다.
- 실제 파일 업로드, S3 저장, 다운로드 URL, 버전, 댓글, 요약/AI 문서 API는 별도 PR로 분리한다.
- API 예시의 자료 상태값 `UPLOADED`, `ARCHIVED`와 현재 DB/코드 enum `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED` 사이 차이는 최종 API 수정본에서 보정이 필요하다.

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

| PR | 제목 | 브랜치 | 확인한 head | 현재 메모 |
|---|---|---|---|---|
| #19 | `feat: 프로젝트룸 멤버 초대 API 추가` | `feature/project-room-members-invitations` | `4a16d9e` | 최신 API Design의 이메일/사용자 ID 초대와 초대 링크 API 기준 재검토 필요 |
| #20 | `feat: 채팅 기본 API 추가` | `feature/chat-basic-api` | `f13a43e` | 기본 채팅 API는 진행됨. `POST /api/chat/direct-rooms` 누락 여부 확인 필요 |
| #21 | `feat: 작업 WBS 기본 API 추가` | `feature/work-task-wbs-api` | `4dd06c6` | 작업/WBS 기본 API는 진행됨. 대시보드 TODO, WBS board, reorder, time-log 분리 여부 확인 필요 |
| #22 | `feat: 일정 기본 API 추가` | `feature/schedule-basic-api` | `3e2a7bf` | 일정 API는 기준과 대체로 맞음. Google Calendar 연동 범위 표기가 기획과 맞는지 확인 필요 |
| #23 | `feat: 프로젝트룸 권한 검사 서비스 분리` | `feature/room-access-service` | `5aa677a` | #22 기준 draft stacked PR 생성됨. GitHub Actions checks 없음. #22 merge 후 `develop` 기준 CI 재확인 필요 |
| #24 | `chore: Google-only 인증 기반 정리` | `feature/auth-google-foundation` | `15f9b7d` | #23 기준 draft stacked PR. 6/25 Google authorize/callback endpoint 보정 완료. GitHub Actions checks 없음. #22, #23 merge 후 `develop` 기준 CI 재확인 필요 |
| #25 | `feat: 자료 기본 저장 조회 API 추가` | `feature/resource-basic-foundation` | `14c522d` | #24 기준 draft stacked PR. 6/25 자료 수정/삭제 보정과 #24 base 병합 완료. GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요 |
| #26 | `feat: 에이전트 저장 기반 추가` | `feature/agent-storage-foundation` | `1382c41` | #25 기준 draft stacked PR. 6/25 agent enum 보정과 #25 base 병합 완료. GitHub Actions checks 없음. #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요 |
| #27 | `test: Entity Flyway 정합성 검사 추가` | `feature/entity-flyway-alignment` | `c4c0581` | #26 기준 draft stacked PR 생성됨. GitHub Actions checks 없음. #22~#26 merge 후 `develop` 기준 CI 재확인 필요 |
| #28 | `ci: stacked PR 테스트 검증 보강` | `feature/testcontainers-ci-foundation` | `35003e0` | #27 기준 draft stacked PR 생성됨. GitHub Actions `build` 통과 |
| #38 | `[test] 엔티티 경계 가드 추가` | `chore/entity-boundary-guards` | `89603fb` | #28 기준 draft stacked PR 생성 완료. 로컬 검증 통과. GitHub Actions `build` 통과 |

## API Design 기준 재검토 후보

| 영역 | API Design 기준 | 현재 판단 |
|---|---|---|
| 프로젝트룸 초대 | `POST /api/project-rooms/{roomId}/invitations`는 이메일 또는 가입 사용자 ID 초대, `POST /api/project-rooms/{roomId}/invite-links`, `GET /api/invite-links/{token}`, `PATCH /api/invite-links/{token}/accept` 포함 | #19가 가입 사용자 ID 초대 중심이면 최신 기준과 차이가 있음 |
| 인증 | 6/25 기준 `GET /api/auth/google/authorize`, `POST /api/auth/google/callback`, refresh, logout | #24에서 signup/email-password는 되살리지 않고, `POST /api/auth/login`을 Google authorize/callback endpoint 뼈대로 보정한다 |
| 자료 상태값 | `ResourceResponse.status` 예시는 `UPLOADED`, `ANALYZING`, `ANALYZED`, `FAILED`, `ARCHIVED` | 현재 데이터 딕셔너리와 코드 enum은 `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED`이므로 최종 API 수정본에서 상태값 명칭 보정 필요 |
| 자료 업로드 | `POST /api/resources`는 개인 또는 프로젝트룸 자료 업로드 | #25는 파일/S3 업로드 전 단계의 자료 카드 메타데이터 저장/조회 기반만 구현함. multipart 업로드, 파일 메타데이터, 버전 생성은 별도 PR 필요 |
| 에이전트 후보 타입 | 기획/가이드는 TODO, WBS, REQUIREMENT, SCHEDULE, QUESTION, CONTRACT_FIELD, CONTRACT_REVIEW, DOCUMENT_DRAFT, DAILY_SUMMARY, MEMO 등 후보를 통합 저장한다고 설명 | #26에서 6/25 기준 타입을 추가했다. 기존 `TASK`, `REVIEW_ITEM`은 09_Data-Model 표와 기존 저장값 호환을 위해 유지했다 |
| Entity/Flyway | `agent_model_call_logs` 엔티티와 Flyway 테이블 정의 | Flyway 정의가 모델 호출 로그가 아니라 agent suggestion 형태 컬럼을 가진 것으로 보인다. 별도 정합성 PR에서 확인 필요 |
| 엔티티 경계 | BaseTimeEntity, global 공통 엔티티, Tauri `local_*` 서버 엔티티 금지 | `chore/entity-boundary-guards`에서 금지 구조를 테스트로 고정한다 |
| 채팅 | `POST /api/chat/direct-rooms` 포함 | #20에 1:1 채팅방 생성/조회 API가 있는지 확인 필요 |
| 작업 대시보드 | `GET /api/dashboard/tasks` 포함 | #21 또는 별도 dashboard 작업으로 분리할지 결정 필요 |
| WBS 작업판 | `GET /api/project-rooms/{roomId}/wbs-board` 포함 | #21에 WBS board 통합 조회가 있는지 확인 필요 |
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
4. #26은 #22, #23, #24, #25 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
5. #27은 #22~#26 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
6. #28은 GitHub Actions CI `build`가 통과했다.
7. #38은 GitHub Actions CI `build`가 통과했다.
8. 다음 추천 작업은 열린 PR stack 정리와 최종 API 수정본 수신 뒤 차이 보정이다.
9. #19~#28은 현재 API 기준으로 계속 진행하되, 수정본 API가 오면 차이 보정한다.

## 현재 API 기준 가능한 작업

| 우선순위 | 작업 | 메모 |
|---|---|---|
| 1 | 패키지 구조와 도메인 위치 점검 | `personal/*`, `work/*`, `project`, `resource`, `agent` 등 기존 구조 기준 |
| 2 | Entity, Enum, Repository, Flyway 기준 점검 | API보다 데이터 딕셔너리를 우선한다 |
| 3 | Security/JWT/CurrentUser 기반 점검 | 모든 API에서 재사용되는 공통 기반 |
| 4 | 공통 응답, 공통 에러, Validation 점검 | API 확정 뒤 Controller에 붙일 기반 |
| 5 | 프로젝트룸 권한 검사 서비스 점검 | `room_members.status=ACTIVE`, `PROJECT_LEADER` 권한 확인 |
| 6 | Testcontainers와 테스트 support 점검 | API 확정 뒤 PR별 테스트를 빠르게 붙이기 위함 |
| 7 | `.http` 파일 구조 정리 | 현재 API 기준 요청 예시를 맞춘다 |

상세 작업 지시와 복붙 프롬프트는 `docs/CURRENT_API_BASELINE_WORK.md`를 따른다.

## 작업 시 주의할 점

| 주의할 점 | 이유 |
|---|---|
| 여러 기능을 한 브랜치에 섞지 않기 | API 수정본이 오면 보정하기 어렵다 |
| 현재 API를 최종 확정이라고 쓰지 않기 | 지금은 작업 기준선이다 |
| 기획/DB와 충돌하는 API는 기록하기 | 현재 API만 보고 무리하게 확정하지 않는다 |
| Agent/RAG payload 과확정 금지 | 에이전트 흐름은 변경 가능성이 높다 |
| WebSocket payload 과확정 금지 | 채팅/이벤트 범위 변경 가능성이 있다 |

## API 명세 완성본 수신 절차

사용자가 새 API 명세서를 주면서 완성본이라고 말하면 아래를 수행한다.

1. 새 API 명세 파일 위치를 확인한다.
2. 기존 `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md`와 비교한다.
3. 새 파일이 기준이면 기준 문서 지도와 스킬의 API 기준 경로를 갱신한다.
4. 이 문서의 `현재 작업 모드`를 새 API 기준 작업 모드로 바꾼다.
5. `API Design 기준 재검토 후보`를 새 API 기준으로 다시 작성한다.
6. 현재 구현과 새 API의 endpoint, DTO, 테스트 차이를 다시 정리한다.
7. 이후 한 PR씩 차이 보정 작업을 하고 로컬 검증과 GitHub Actions CI를 확인한다.

## 작업 완료 기준

문서만 수정한 경우:

- `ls docs/00_BACKEND_START_HERE.md docs/WORK_HANDOFF.md docs/CODEX_BACKEND_WORKFLOW.md`
- `rg "10_API-Design.md|최종기획|데이터딕셔너리|WORK_HANDOFF|GitHub Actions|CI" docs`
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
