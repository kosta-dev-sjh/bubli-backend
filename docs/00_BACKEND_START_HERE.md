# Bubli Backend Start Here

Last checked: 2026-06-24 20:33:25 KST

이 문서는 백엔드 작업을 시작할 때 가장 먼저 보는 입구다.
`10_API-Design.md`는 API 계약서일 뿐이고, 서비스 의도와 DB 기준과 코드 규칙은 각각 다른 기준 문서에서 확인한다.

현재 API 명세서는 최종 수정본이 아닐 수 있다.
그래도 작업을 멈추지 않는다.
현재 `10_API-Design.md`를 작업 기준선으로 삼아 백엔드 뼈대와 기본 API를 구현하고, 수정본이 오면 차이 보정 작업을 한다.

## 기준 문서 지도

| 확인할 것 | 기준 문서 | 용도 |
|---|---|---|
| 전체 작업 규칙 | `/Users/maren/EDU/Final Project/AGENTS.md` | Bubli 전체 방향, 금지사항, 최신 문서 찾는 법 |
| 서비스 의도와 기능 방향 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/Bubli_최종기획_완성본_v15_DB회의반영_2026-06-24.md` | 왜 이 기능이 있는지, 사용자가 어떤 흐름으로 쓰는지 확인 |
| DB 테이블, 컬럼, 관계 | `/Users/maren/EDU/Final Project/04_개발_작업공간/DB_팀검토_2026-06-23/Bubli_DB_검토보드/09_데이터딕셔너리_회의반영_2026-06-24.html` | 엔티티, FK/UK, NULL, 상태값 기준 확인 |
| API 계약 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md` | 엔드포인트, 요청/응답, 프론트와 맞출 API 기준 확인 |
| 백엔드 구현 규칙 | `docs/Bubli_백엔드_개발_가이드_2026-06-24.md` | 패키지 구조, 계층 책임, 권한 검사, 테스트 기준 확인 |
| 현재 작업 현황 | `docs/WORK_HANDOFF.md` | 열린 PR, 현재 불일치 후보, 다음 작업 순서 확인 |
| Codex/Claude 작업 절차 | `docs/CODEX_BACKEND_WORKFLOW.md` | AI에게 작업시킬 때 반복해서 지킬 순서 확인 |
| 현재 API 기준 작업 계획 | `docs/CURRENT_API_BASELINE_WORK.md` | 수정본 API가 오기 전까지 실제로 구현할 작업 순서 |

## 작업 종류별로 볼 문서

| 작업 종류 | 먼저 볼 문서 |
|---|---|
| 새 API 구현 | 최종 기획문서 -> 데이터 딕셔너리 -> `10_API-Design.md` -> 백엔드 개발 가이드 -> 현재 API 기준 작업 계획 |
| 기존 PR 검토 | `WORK_HANDOFF.md` -> `10_API-Design.md` 관련 섹션 -> 실제 컨트롤러/서비스 코드 |
| 엔티티나 마이그레이션 수정 | 데이터 딕셔너리 -> 백엔드 개발 가이드 -> 기존 엔티티 패턴 |
| 패키지 구조 수정 | 백엔드 개발 가이드 -> 기존 패키지 구조 |
| 테스트 보강 | 백엔드 개발 가이드 -> 기존 테스트 코드 -> `.http` 예시 |
| AI/RAG 작업 | 최종 기획문서의 에이전트 흐름 -> 데이터 딕셔너리의 `ai_documents`, `agent_jobs`, `agent_suggestions` 계열 -> API Design의 AI 섹션 |

## 현재 API 기준 작업 모드

API 명세서가 아직 덜 수정된 상태라도 아래 작업은 진행한다.
작업 기준은 현재 `10_API-Design.md`다.

| 진행 작업 | 이유 |
|---|---|
| 패키지 구조 정리 | API 주소와 무관하게 유지된다 |
| Gradle 의존성, profile, 설정 정리 | API 명세가 바뀌어도 거의 영향이 없다 |
| JPA Entity, Enum, Repository 점검 | 데이터 딕셔너리가 기준이다 |
| Flyway migration 점검 | DB 기준을 먼저 안정화한다 |
| Spring Security, JWT, CurrentUser 기반 | 모든 API가 공통으로 사용한다 |
| 공통 응답, 공통 에러, Validation | API 모양이 바뀌어도 재사용된다 |
| 권한 검사 서비스 | 프로젝트룸 멤버 권한은 여러 기능의 공통 기반이다 |
| Testcontainers, 테스트 support | 이후 API 확정 뒤 검증을 빠르게 한다 |
| 현재 API 기준 Controller/DTO/API 테스트 | 최종 수정본이 오기 전까지 기능 뼈대를 만든다 |
| `.http` 요청 예시 | 현재 API 기준으로 프론트와 맞춰본다 |

수정본 API가 오기 전에는 아래만 주의한다.

| 주의할 점 | 이유 |
|---|---|
| 여러 기능을 한 브랜치에 섞지 않기 | 나중에 API 수정본이 오면 보정하기 어렵다 |
| 현재 API를 최종 확정이라고 쓰지 않기 | 지금은 작업 기준선이다 |
| 기획/DB와 충돌하는 API는 `WORK_HANDOFF.md`에 남기기 | 무리하게 구현하면 나중에 더 크게 고친다 |
| 에이전트/RAG payload를 과하게 확정하지 않기 | 구조 변경 가능성이 높다 |
| WebSocket payload를 과하게 확정하지 않기 | 채팅/이벤트 범위 변경 가능성이 있다 |

## API 명세서 완성본이 왔을 때

사용자가 "API 명세서가 왔다", "API 명세 완성본이다", "이 파일이 최종 API다"라고 말하면 아래 순서로 전환한다.

1. 새 API 명세 파일의 위치와 파일명을 확인한다.
2. 기존 기준 문서와 비교한다.
3. 새 파일이 기준이면 `docs/00_BACKEND_START_HERE.md`, `docs/WORK_HANDOFF.md`, `docs/CODEX_BACKEND_WORKFLOW.md`, `docs/Bubli_백엔드_개발_가이드_2026-06-24.md`의 API 기준 경로를 갱신한다.
4. `/Users/maren/.codex/skills/bubli-backend-workflow/SKILL.md`의 API 기준 경로도 갱신한다.
5. `docs/WORK_HANDOFF.md`의 PR 재검토 후보를 새 API 기준으로 다시 정리한다.
6. 기존 구현을 버리지 말고 차이 나는 부분만 보정한다.

## 고정 원칙

- API Design만 보고 구현하지 않는다.
- 최종 기획문서, 데이터 딕셔너리, API Design, 백엔드 개발 가이드를 함께 본다.
- 한 번에 한 기능 또는 한 PR만 다룬다.
- `global`에는 설정, 응답, 에러, 보안, 검증 같은 공통 코드만 둔다.
- 공통 엔티티나 `BaseTimeEntity`를 만들지 않는다. `createdAt`, `updatedAt`은 각 엔티티에 직접 둔다.
- Tauri SQLite의 `local_*` 테이블은 서버 JPA 엔티티로 만들지 않는다.
- 코드 작업 완료 보고는 로컬 검증과 GitHub Actions CI 확인 뒤에만 한다.

## 다음 작업 시작 순서

1. `git status --short --branch`로 현재 브랜치와 변경사항을 확인한다.
2. `docs/WORK_HANDOFF.md`에서 현재 PR 상태와 다음 작업을 확인한다.
3. 해당 기능의 기준 문서를 위 표 순서대로 읽는다.
4. 기존 코드에서 같은 패턴의 controller, service, repository, dto, entity를 확인한다.
5. 한 기능만 수정한다.
6. 로컬 검증과 CI 확인 뒤 `docs/WORK_HANDOFF.md`를 갱신한다.
