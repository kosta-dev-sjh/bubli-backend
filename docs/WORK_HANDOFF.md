# Bubli Backend Work Handoff

Last checked: 2026-06-25 01:19 KST

이 문서는 백엔드 현재 상태를 이어받기 위한 인수인계 문서다.
작업이 끝날 때마다 이 문서의 PR 상태, 확인 결과, 다음 작업을 갱신한다.

## 현재 기준

| 항목 | 값 |
|---|---|
| 로컬 레포 | `/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend` |
| 현재 확인 브랜치 | `feature/resource-basic-foundation` |
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

- `./gradlew compileTestJava` 통과 (2026-06-25 00:49 KST)
- `./gradlew cleanTest test` 통과 (2026-06-25 00:49 KST)
- `git diff --check` 통과 (2026-06-25 00:49 KST)
- 엔티티 44개, Repository 4개, Controller 4개, Service 5개 확인
- 현재 API 기준 세부 작업 지시는 `docs/CURRENT_API_BASELINE_WORK.md`를 기준으로 나눈다.

## 최근 완료 작업

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
| #24 | `chore: Google-only 인증 기반 정리` | `feature/auth-google-foundation` | `local latest` | #23 기준 draft stacked PR. 6/25 Google authorize/callback endpoint 보정 중. GitHub Actions checks 없음. #22, #23 merge 후 `develop` 기준 CI 재확인 필요 |
| #25 | `feat: 자료 기본 저장 조회 API 추가` | `feature/resource-basic-foundation` | `local latest` | #24 기준 draft stacked PR. #24 최신 변경 병합 중. GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요 |

## API Design 기준 재검토 후보

| 영역 | API Design 기준 | 현재 판단 |
|---|---|---|
| 프로젝트룸 초대 | `POST /api/project-rooms/{roomId}/invitations`는 이메일 또는 가입 사용자 ID 초대, `POST /api/project-rooms/{roomId}/invite-links`, `GET /api/invite-links/{token}`, `PATCH /api/invite-links/{token}/accept` 포함 | #19가 가입 사용자 ID 초대 중심이면 최신 기준과 차이가 있음 |
| 인증 | 6/25 기준 `GET /api/auth/google/authorize`, `POST /api/auth/google/callback`, refresh, logout | #24에서 signup/email-password는 되살리지 않고, `POST /api/auth/login`을 Google authorize/callback endpoint 뼈대로 보정한다 |
| 자료 상태값 | `ResourceResponse.status` 예시는 `UPLOADED`, `ANALYZING`, `ANALYZED`, `FAILED`, `ARCHIVED` | 현재 데이터 딕셔너리와 코드 enum은 `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED`이므로 최종 API 수정본에서 상태값 명칭 보정 필요 |
| 자료 업로드 | `POST /api/resources`는 개인 또는 프로젝트룸 자료 업로드 | #25는 파일/S3 업로드 전 단계의 자료 카드 메타데이터 저장/조회 기반만 구현함. multipart 업로드, 파일 메타데이터, 버전 생성은 별도 PR 필요 |
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
4. `docs/CURRENT_API_BASELINE_WORK.md` 기준으로 다음 작업 1개를 선택한다.
5. 추천 다음 작업은 `agent` 저장 기반이다. `work.task`, `work.wbs`, `chat`은 이미 #21, #20으로 열린 상태라 새 구현을 섞지 않는다.
6. #19~#25는 현재 API 기준으로 계속 진행하되, 수정본 API가 오면 차이 보정한다.

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
