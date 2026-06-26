# Codex Backend Workflow

이 문서는 Codex, Claude, 팀원이 백엔드 작업을 이어받을 때 쓰는 고정 절차다.
목표는 긴 설명을 매번 다시 넣지 않고도 같은 기준으로 작업하게 만드는 것이다.

## 시작할 때

1. `/Users/maren/EDU/Final Project/AGENTS.md`를 먼저 확인한다.
2. `docs/00_BACKEND_START_HERE.md`를 확인한다.
3. `docs/WORK_HANDOFF.md`에서 현재 PR 상태와 다음 작업을 확인한다.
4. 현재 브랜치와 변경사항을 확인한다.

```bash
git status --short --branch
```

## 기준 문서 읽는 순서

작업 대상 기능이 정해지면 아래 순서로 필요한 부분만 읽는다.

1. 최종 기획문서에서 기능 의도와 사용자 흐름을 확인한다.
2. 데이터 딕셔너리에서 테이블, 컬럼, 상태값, FK/UK/NULL 기준을 확인한다.
3. `10_API-Design.md`에서 API 주소, 요청, 응답 기준을 확인한다.
4. 백엔드 개발 가이드에서 패키지 위치, 계층 책임, 권한 검사, 테스트 기준을 확인한다.
5. 실제 코드에서 같은 패턴의 controller, service, repository, dto, entity를 확인한다.

## 현재 API 기준 작업

API 명세서가 아직 덜 수정된 상태라도 작업을 멈추지 않는다.
현재 `10_API-Design.md`를 작업 기준선으로 삼아 백엔드 뼈대와 기본 API를 구현한다.
수정본 API가 오면 차이 보정 작업으로 처리한다.

- 패키지 구조와 도메인 위치 점검
- Entity, Enum, Repository, Flyway 기준 점검
- Spring Security, JWT, CurrentUser 기반 점검
- 공통 응답, 공통 에러, Validation 점검
- 프로젝트룸 권한 검사 서비스 점검
- Testcontainers와 테스트 support 점검
- 현재 API 기준 Controller/DTO/Service/Repository/Test 구현
- `.http` 요청 예시 정리

이 모드에서는 구현을 진행한다.
다만 기획/DB와 충돌하는 API나 불확실한 판단은 `docs/WORK_HANDOFF.md`에 남긴다.

## API 명세 완성본이 들어왔을 때

사용자가 "API 명세서가 왔다", "API 명세 완성본이다", "이 파일이 최종 API다"라고 말하면 먼저 문서와 스킬을 갱신한다.

1. 새 API 명세 파일 위치를 확인한다.
2. 기존 API 기준 문서와 차이를 확인한다.
3. 기준 파일이 바뀌면 아래 파일의 API 기준 경로를 갱신한다.
   - `docs/00_BACKEND_START_HERE.md`
   - `docs/WORK_HANDOFF.md`
   - `docs/CODEX_BACKEND_WORKFLOW.md`
   - `docs/Bubli_백엔드_개발_가이드_2026-06-24.md`
   - `/Users/maren/.codex/skills/bubli-backend-workflow/SKILL.md`
4. `docs/WORK_HANDOFF.md`의 현재 작업 모드를 새 API 기준 작업 모드로 바꾼다.
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
rg "10_API-Design.md|최종기획|데이터딕셔너리|WORK_HANDOFF|GitHub Actions|CI" docs
git diff --check
```

추가로 예전 다운로드 폴더의 API Design 절대경로가 문서나 코드에 남아 있지 않은지 확인한다.

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

## PR 재검토 순서

1. #19 프로젝트룸 멤버/초대 API
2. #20 채팅 기본 API
3. #21 작업/WBS 기본 API
4. #22 일정 기본 API
5. 자료/AI 문서/RAG API
