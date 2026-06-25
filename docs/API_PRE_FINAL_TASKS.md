# API 확정 전 백엔드 작업 카드

Last checked: 2026-06-24 20:04:48 KST

이 문서는 API 확정 전에도 안전한 기반 작업만 모아둔 보조 문서다.
현재 기본 작업 기준은 `docs/CURRENT_API_BASELINE_WORK.md`다.
이제는 현재 `10_API-Design.md`를 작업 기준선으로 삼아 구현을 진행하고, 수정본 API가 오면 차이 보정 작업으로 처리한다.

현재 API 명세는 최종 확정 전으로 본다.
따라서 새 Controller endpoint와 Request/Response DTO를 대량으로 만들지 않고, API가 바뀌어도 재사용되는 기반 작업만 맡긴다.

## 현재 점검 결과

| 항목 | 현재 상태 | 판단 |
|---|---|---|
| 컴파일 | `./gradlew compileTestJava` 통과 | 현재 기준 코드는 컴파일 가능 |
| 열린 PR | #19, #20, #21, #22 open | API 확정 전에는 수정보다 기준 충돌 후보 정리가 먼저 |
| 패키지 구조 | `personal/*`, `work/*` 하위 도메인 구조 존재 | 큰 방향은 백엔드 가이드와 맞음 |
| Entity | 44개 존재 | DB 뼈대는 넓게 들어와 있음 |
| Repository | 4개 존재 | 실제 기능 연결용 Repository는 아직 일부만 있음 |
| Controller | 4개 존재 | `auth`, `user`, `project`, `work.schedule` 중심 |
| Service | 4개 존재 | 인증은 TODO 상태, project/schedule은 일부 동작 |
| Flyway | `V1__init_schema.sql` 존재 | pgvector와 주요 테이블 생성 기준 있음 |
| Testcontainers | `PostgresIntegrationTestSupport`, `EntityMappingTest` 존재 | 통합 테스트 기반 있음 |
| `.http` | `auth`, `project-rooms`, `schedules` 존재 | API 확정 뒤 보강 필요 |

## 지금 시키면 좋은 순서

1. 공통 기반 점검 문서화
2. 권한 검사 서비스 분리
3. 인증 기반 정리
4. Repository 최소 보강
5. Entity/Flyway 정합성 점검
6. 테스트 기반 정리
7. `.http`와 수동 검증 규칙 정리

## 작업 카드 1. 공통 기반 점검

목표:

- 공통 응답, 에러, Validation, Security, Trace 구조가 팀원이 따라 쓸 수 있는지 확인한다.
- 코드를 크게 바꾸지 않고 부족한 부분을 목록화한다.

맡길 사람:

- 백엔드 기본 구조를 보는 사람.

작업 범위:

- `global/response`
- `global/error`
- `global/security`
- `global/trace`
- `global/validation`
- `docs/Bubli_백엔드_개발_가이드_2026-06-24.md`

완료 기준:

- 공통 응답과 에러 형식이 문서와 코드에서 같은지 정리한다.
- 도메인별로 추가가 필요한 `ErrorCode` 후보를 정리한다.
- `WORK_HANDOFF.md`에 발견 사항을 남긴다.
- 코드 수정이 없으면 `git diff --check`만 확인한다.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

Controller/DTO는 만들지 말고, global 공통 기반만 점검해줘.
범위는 global/response, global/error, global/security, global/trace, global/validation이야.

공통 응답, 공통 에러, Validation, 인증 실패 응답, traceId 흐름이
백엔드 개발 가이드와 맞는지 확인하고,
부족한 점과 다음 작업 후보를 docs/WORK_HANDOFF.md에 정리해줘.
코드 수정은 하지 말고 git diff --check까지 확인해줘.
```

## 작업 카드 2. 프로젝트룸 권한 검사 서비스 분리

목표:

- 여러 기능에서 반복되는 `room_members.status=ACTIVE` 확인을 재사용 가능한 서비스로 분리한다.
- API가 확정되지 않아도 WBS, 일정, 자료, 채팅, 위젯에서 계속 쓰는 기반이다.

맡길 사람:

- 프로젝트룸/권한 흐름을 맡을 사람.

작업 범위:

- `project/service`
- `project/repository/RoomMemberRepository.java`
- 현재 `ProjectRoomService`, `ScheduleService`의 중복 권한 검사

완료 기준:

- 예: `ProjectRoomAccessService` 또는 `RoomMemberPermissionService` 생성.
- ACTIVE 멤버 확인 메서드 제공.
- PROJECT_LEADER 확인 메서드는 API 확정 전이라도 기반으로 추가 가능.
- 기존 `ProjectRoomService`, `ScheduleService`가 새 서비스를 사용.
- 단위 테스트 추가.
- `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check` 통과.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

새 Controller endpoint는 만들지 말고,
프로젝트룸 권한 검사 기반만 정리해줘.

목표:
- ProjectRoomService와 ScheduleService에 흩어진 ACTIVE 멤버 확인을 공통 서비스로 분리
- room_members.status=ACTIVE 확인 메서드 제공
- PROJECT_LEADER 확인 메서드는 API 확정 전에도 쓸 수 있게 기반만 제공
- 기존 서비스가 새 권한 검사 서비스를 사용하도록 수정
- 테스트 추가

검증:
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check

끝나면 docs/WORK_HANDOFF.md도 갱신해줘.
```

## 작업 카드 3. 인증 기반 정리

목표:

- 구글 로그인만 쓰는 현재 방향과 인증 코드/문서의 말이 충돌하지 않게 한다.
- 실제 Google ID token 검증은 프론트 흐름과 API 확정 뒤 붙인다.

맡길 사람:

- 인증/Security 담당자.

작업 범위:

- `auth/package-info.java`
- `auth/service/AuthService.java`
- `global/security/SecurityConfig.java`
- `docs/http/auth.http`
- `user_sessions`와 refresh token TODO

현재 발견:

- `auth/package-info.java`에 이메일/비밀번호 회원가입 설명이 남아 있다.
- `SecurityConfig`에 `/api/auth/signup` permitAll이 남아 있다.
- `AuthService`는 Google ID token 검증 TODO 상태다.

완료 기준:

- 문서/주석에서 이메일/비밀번호 가입 기준 제거.
- Google-only 인증 방향으로 TODO 정리.
- 실제 엔드포인트 구현은 API 확정 뒤로 보류.
- 필요하면 signup permitAll 제거 여부를 검토 후보로 남김.
- 코드 수정이 있다면 테스트까지 확인.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

인증은 구글 로그인만 쓰는 방향으로 정리해줘.
다만 실제 Google ID token 검증 구현이나 새 API 확정은 하지 마.

점검/정리 범위:
- auth/package-info.java의 이메일/비밀번호 설명 제거
- AuthService TODO를 Google-only 흐름으로 정리
- SecurityConfig의 /api/auth/signup 허용이 현재 방향과 맞는지 검토
- docs/http/auth.http의 설명이 현재 방향과 맞는지 확인

필요한 최소 수정만 하고,
검증은 ./gradlew compileTestJava, ./gradlew cleanTest test, git diff --check까지 해줘.
끝나면 docs/WORK_HANDOFF.md 갱신해줘.
```

## 작업 카드 4. Repository 최소 보강

목표:

- 엔티티는 있는데 Repository가 없는 도메인 중, API 확정 전에도 필요한 조회 기반을 최소로 만든다.
- 비즈니스 API는 만들지 않는다.

맡길 사람:

- JPA/Repository 담당자.

작업 범위 후보:

- `project`: `Invitation`, `ProjectRoomEvent`
- `work`: `Task`, `WbsItem`
- `personal`: `Memo`, `TimeLog`, `Notification`
- `resource`: `Resource`, `ResourceFile`, `ResourceComment`
- `agent`: `AgentJob`, `AgentSuggestion`
- `chat`: `ChatRoom`, `ChatRoomMember`, `ChatMessage`

주의:

- 모든 Repository를 한 번에 만들지 않는다.
- 열린 PR과 다음 작업에 필요한 것만 묶어서 만든다.
- API가 확정되지 않은 조회 메서드는 과하게 만들지 않는다.

추천 분할:

- 4-1: `work.task`, `work.wbs` Repository
- 4-2: `project` Invitation/Event Repository
- 4-3: `resource` 기본 Repository
- 4-4: `agent` job/suggestion Repository
- 4-5: `chat` 기본 Repository

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

새 Controller/DTO는 만들지 말고 Repository 최소 보강만 해줘.
이번에는 [여기에 도메인 하나만 적기]만 처리해.

규칙:
- JpaRepository 기본 인터페이스 중심
- API 확정 전이라 복잡한 쿼리 메서드는 만들지 않기
- 필요한 단순 exists/find 정도만 추가
- Repository 테스트가 필요하면 최소로 추가

검증:
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check

끝나면 docs/WORK_HANDOFF.md 갱신해줘.
```

## 작업 카드 5. Entity/Flyway 정합성 점검

목표:

- JPA Entity와 `V1__init_schema.sql`이 서로 맞는지 확인한다.
- API 확정 전이라도 DB 매핑 오류는 먼저 잡는다.

맡길 사람:

- DB/JPA 꼼꼼히 보는 사람.

작업 범위:

- `src/main/java/com/bubli/**/entity/*.java`
- `src/main/resources/db/migration/V1__init_schema.sql`
- `EntityMappingTest`
- 데이터 딕셔너리 최신본

완료 기준:

- 컬럼명, enum varchar, nullable, unique 제약이 크게 어긋나는 부분 목록화.
- `EntityMappingTest`가 Testcontainers 기준으로 통과하는지 확인.
- API 불확정과 무관한 매핑 오류만 수정.
- 수정하면 테스트까지 확인.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

Entity와 Flyway 정합성만 점검해줘.
Controller/DTO/API는 건드리지 마.

범위:
- src/main/java/com/bubli/**/entity/*.java
- src/main/resources/db/migration/V1__init_schema.sql
- EntityMappingTest
- 최신 데이터 딕셔너리

매핑 오류나 명확한 DB 불일치만 수정하고,
불확실한 기획/API 문제는 docs/WORK_HANDOFF.md에 후보로 남겨줘.

검증:
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check
```

## 작업 카드 6. Testcontainers/CI 테스트 기반 정리

목표:

- 팀원이 기능 PR을 만들 때 같은 방식으로 통합 테스트를 붙일 수 있게 한다.
- CI와 로컬 Docker 환경 차이를 줄인다.

맡길 사람:

- 테스트/DevOps 감각 있는 사람.

작업 범위:

- `.github/workflows/ci.yml`
- `PostgresIntegrationTestSupport`
- `EntityMappingTest`
- 기존 Controller integration test
- `docs/Bubli_백엔드_개발_가이드_2026-06-24.md` 테스트 섹션

완료 기준:

- 통합 테스트 베이스 사용 방법 정리.
- 외부 datasource가 있을 때와 없을 때의 동작 확인.
- `./gradlew cleanTest test` 통과.
- CI 실패 시 원인 추적 방법을 `WORK_HANDOFF.md`에 남김.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

Testcontainers와 CI 테스트 기반만 점검해줘.
새 기능 API는 만들지 마.

범위:
- .github/workflows/ci.yml
- PostgresIntegrationTestSupport
- EntityMappingTest
- 기존 ControllerIntegrationTest

목표:
- 팀원이 같은 방식으로 통합 테스트를 붙일 수 있는지 확인
- 로컬 Docker 없음/있음, CI PostgreSQL service 환경 차이 확인
- 필요한 문서 보강

검증:
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check

끝나면 docs/WORK_HANDOFF.md 갱신해줘.
```

## 작업 카드 7. `.http` 수동 검증 기준 정리

목표:

- API 확정 뒤 프론트/백엔드가 같은 방식으로 수동 검증하게 한다.
- 지금은 확정 안 된 엔드포인트를 추가하지 않고 형식만 정리한다.

맡길 사람:

- 프론트와 소통할 사람 또는 문서 담당.

작업 범위:

- `docs/http/auth.http`
- `docs/http/project-rooms.http`
- `docs/http/schedules.http`
- 필요하면 `docs/http/README.md`

완료 기준:

- 토큰 변수, baseUrl, roomId 같은 변수 사용 방식 통일.
- API 확정 전인 요청에는 TODO 표시.
- 민감정보를 직접 넣지 않는다는 기준 명시.
- `git diff --check` 통과.

복붙 프롬프트:

```text
bubli-backend-workflow로 진행해줘.
API 명세 확정 전 모드야.

docs/http 수동 검증 파일만 정리해줘.
새 endpoint 예시는 추가하지 말고, 기존 auth/project-rooms/schedules 형식과 변수 사용만 통일해줘.

필요하면 docs/http/README.md를 만들어서
baseUrl, accessToken, refreshToken, roomId 사용법과 민감정보 금지 기준을 적어줘.

검증:
git diff --check

끝나면 docs/WORK_HANDOFF.md 갱신해줘.
```

## 지금 당장 추천 분배

| 담당 후보 | 추천 작업 | 이유 |
|---|---|---|
| 백엔드 중심 담당 | 작업 카드 2. 프로젝트룸 권한 검사 서비스 분리 | 여러 PR의 공통 기반이라 효과가 큼 |
| 인증 담당 | 작업 카드 3. 인증 기반 정리 | 구글 로그인-only 방향은 이미 정해짐 |
| JPA 담당 | 작업 카드 5. Entity/Flyway 정합성 점검 | API 확정 전에도 안전하게 가능 |
| 테스트/DevOps 담당 | 작업 카드 6. Testcontainers/CI 테스트 기반 정리 | 모든 PR의 완료 기준이 됨 |
| 문서/프론트 협업 담당 | 작업 카드 7. `.http` 수동 검증 기준 정리 | API 확정 뒤 프론트와 맞추기 쉬움 |
| 에이전트 담당 | 작업 카드 4-4. `agent` Repository 최소 보강 | RAG 구현 전 저장 기반을 먼저 확인 |

## 금지

- API 확정 전에는 새 Controller endpoint를 많이 만들지 않는다.
- Request/Response DTO를 최종이라고 가정하지 않는다.
- 에이전트가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하게 만들지 않는다.
- `global` 공통 엔티티나 `BaseTimeEntity`를 만들지 않는다.
- `local_*` Tauri SQLite 테이블을 서버 JPA 엔티티로 만들지 않는다.
