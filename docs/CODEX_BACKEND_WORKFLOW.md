# Codex Backend Workflow

이 문서는 Codex, Claude, 팀원이 백엔드 작업을 이어받을 때 쓰는 고정 절차다.
목표는 긴 설명을 매번 다시 넣지 않고도 같은 기준으로 작업하게 만드는 것이다.

## 시작할 때

1. `/Users/maren/EDU/Final Project/AGENTS.md`를 먼저 확인한다.
2. `docs/00_BACKEND_START_HERE.md`를 확인한다.
3. `docs/WORK_HANDOFF.md`에서 현재 PR 상태와 다음 작업을 확인한다.
4. 열린 PR을 리뷰하거나 병합 판단을 해야 하면 `docs/API_SKELETON_PR_REVIEW_GUIDE.md`를 확인한다.
5. 현재 브랜치와 변경사항을 확인한다.

```bash
git status --short --branch
```

## 기준 문서 읽는 순서

작업 대상 기능이 정해지면 아래 순서로 필요한 부분만 읽는다.

1. 최종 기획문서에서 기능 의도와 사용자 흐름을 확인한다.
2. `09_Data-Model.md`에서 서버 DB 테이블, 컬럼, 상태값, FK/UK/NULL 기준을 확인한다.
3. `10_API-Design.md`에서 API 주소, 요청, 응답 기준을 확인한다.
4. 백엔드 개발 가이드에서 패키지 위치, 계층 책임, 권한 검사, 테스트 기준을 확인한다.
5. 실제 코드에서 같은 패턴의 controller, service, repository, dto, entity를 확인한다.

## 2026-06-25 최신 기준 작업

2026-06-25에 들어온 `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`, `Bubli_백엔드_개발_가이드_2026-06-25.md`를 현재 최신 기준으로 본다.
2026-06-25 12:16 KST 기준으로 `/Users/maren/Downloads/10_API-Design (1).md`가 canonical `10_API-Design.md`에 다시 반영됐다.
현재 API 기준 해시는 `13d0453f574dbd60cb598a3502b9be680640f897ce9429ec6ba10cf9c5ce336b`다.
기존 6/24 기준으로 만든 구현과 PR은 버리지 않고, 차이 나는 부분만 보정 작업으로 처리한다.

- 패키지 구조와 도메인 위치 점검
- Entity, Enum, Repository, Flyway 기준 점검
- Spring Security, JWT, CurrentUser 기반 점검
- 공통 응답, 공통 에러, Validation 점검
- 프로젝트룸 권한 검사 서비스 점검
- Testcontainers와 테스트 support 점검
- 6/25 기준 Controller/DTO/Service/Repository/Test 구현
- `.http` 요청 예시 정리
- Data Model 기준 enum 정합성 점검
- API 전용 상태값과 DB 상태값 분리 점검
- ArchUnit 계층/도메인 경계 테스트 통과 점검

이 모드에서는 구현을 진행한다.
다만 기획/DB와 충돌하는 API나 불확실한 판단은 `docs/WORK_HANDOFF.md`에 남긴다.

## ArchUnit 기준 작업

개발 가이드는 참고 문서가 아니라 테스트 통과 기준이다.
기존 코드 패턴이 개발 가이드와 충돌하면 개발 가이드를 우선한다.

ArchUnit 실패는 아래 순서로 고친다.

1. Service가 Controller Request DTO를 직접 받는 구조를 Command, Query, Context, Result로 분리한다.
2. 다른 도메인의 Repository 직접 의존을 제거하고 해당 도메인의 PublicService를 사용한다.
3. 다른 도메인의 Entity 직접 의존을 제거하고 id, boolean, Result, Context 같은 값으로 교환한다.
4. global 패키지가 특정 도메인에 의존하지 않게 한다.
5. agent 도메인이 다른 도메인의 Repository, Entity, 내부 구현에 직접 의존하지 않게 한다.

PR 생성 전 코드 작업이면 아래를 우선 확인한다.

```bash
./gradlew test --tests '*ArchitectureTest'
```

## 새 기준 문서가 다시 들어왔을 때

사용자가 "API 명세서가 왔다", "API 명세 완성본이다", "이 파일이 최종 API다"라고 말하면 먼저 문서와 스킬을 갱신한다.

1. 새 기준 파일 위치를 확인한다.
2. 기존 6/25 기준 문서와 차이를 확인한다.
3. 기준 파일이 바뀌면 아래 파일의 기준 경로를 갱신한다.
   - `docs/00_BACKEND_START_HERE.md`
   - `docs/WORK_HANDOFF.md`
   - `docs/CODEX_BACKEND_WORKFLOW.md`
   - `docs/Bubli_백엔드_개발_가이드_2026-06-25.md`
   - `/Users/maren/.codex/skills/bubli-backend-workflow/SKILL.md`
4. `docs/WORK_HANDOFF.md`의 현재 작업 모드를 새 기준 작업 모드로 바꾼다.
5. 현재 구현과 새 API의 endpoint, DTO, 테스트 차이를 다시 정리한다.
6. 그 다음 한 PR씩 차이 보정 작업을 한다.

## 작업 규칙

- 한 번에 한 PR 또는 한 기능만 다룬다.
- API Design만 보고 구현하지 않는다.
- 새 기능은 기존 패키지 관례를 따른다.
- `global`에는 공통 설정, 응답, 에러, 보안, 검증 코드만 둔다.
- 공통 엔티티와 `BaseTimeEntity`를 만들지 않는다.
- `createdAt`, `updatedAt`은 각 엔티티에 직접 둔다.
- Tauri SQLite의 `local_*` 테이블은 서버 JPA 엔티티로 만들지 않는다.
- 기획/DB와 충돌하는 API는 `WORK_HANDOFF.md`의 재검토 후보에 남긴다.

## 구현할 때

1. 기준 브랜치를 확인하고 필요한 브랜치로 이동한다.
2. 관련 API와 DB 기준을 확인한다.
3. 기존 코드 패턴을 따라 최소 범위로 수정한다.
4. 필요한 테스트를 추가하거나 기존 테스트를 보강한다.
5. `.http` 예시가 필요한 API면 `docs/http`에 추가한다.
6. 작업 중 바뀐 기준이나 남은 위험을 `docs/WORK_HANDOFF.md`에 반영한다.

## 검증할 때

문서만 수정한 경우:

```bash
ls docs/00_BACKEND_START_HERE.md docs/WORK_HANDOFF.md docs/CODEX_BACKEND_WORKFLOW.md
rg "09_Data-Model.md|09C_DB-Tauri-SQLite.md|10_API-Design.md|WORK_HANDOFF|GitHub Actions|CI" docs --glob '!docs/archive/**'
git diff --check
```

추가로 예전 다운로드 폴더의 API Design 절대경로와 6/24 기준 문서 참조가 활성 문서에 남아 있지 않은지 확인한다.

코드를 수정한 경우:

```bash
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check
```

그 뒤 GitHub PR 화면에서 GitHub Actions CI가 통과했는지 확인한다.
CI가 아직 돌고 있거나 실패했다면 완료로 보고하지 않는다.

## 보고할 때

보고에는 아래만 짧게 쓴다.

- 어떤 기준 문서를 확인했는지
- 어떤 파일을 바꿨는지
- 로컬 검증 결과
- GitHub Actions CI 결과
- 다음에 이어서 할 작업

## 팀원에게 PR을 설명할 때

현재 API PR은 완성 기능이 아니라 컴파일되고 테스트되는 기본 API 골격이라고 설명한다.
Controller, DTO, Service, Repository, Entity, Test, `.http` 예시는 들어갈 수 있다.
하지만 외부 연동, 복잡한 정책, 화면별 세부 응답, 실제 AI/RAG 실행, WebSocket 실시간 처리는 각 담당자가 이어서 붙일 부분으로 남긴다.

열린 PR이 많을 때는 `docs/API_SKELETON_PR_REVIEW_GUIDE.md`의 순서대로 본다.
먼저 `develop` 대상이고 CI가 통과한 PR을 리뷰하고, stacked PR은 base 순서대로 확인한다.

## PR 재검토 순서

1. #19~#22 develop 대상 기본 API PR
2. #23~#28 stacked foundation PR
3. 자료, 에이전트, Entity/Flyway, CI 보강 PR
4. 6/25 기준과 맞지 않는 DTO, endpoint, enum, 권한 검사 보정 PR
