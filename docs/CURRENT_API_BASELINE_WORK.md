# 2026-06-25 최신 기준 백엔드 보정 계획

Last checked: 2026-06-25 18:35 KST

2026-06-25에 최신 `09_Data-Model.md`, `09C_DB-Tauri-SQLite.md`, `10_API-Design.md`, `Bubli_백엔드_개발_가이드_2026-06-25.md`가 들어왔다.
2026-06-25 12:16 KST에 `/Users/maren/Downloads/10_API-Design (1).md`가 새 API 기준으로 다시 반영됐다.
2026-06-25 18:35 KST에 `/Users/maren/Downloads/Bubli_백엔드_개발_가이드_2026-06-25 (1).md`의 Service 인터페이스화 기준을 현재 백엔드 개발 가이드에 반영했다.
이 문서는 기존 #19~#81 PR과 현재 백엔드 코드를 새 기준에 맞게 다시 검토하고 보정하는 작업 계획이다.

현재 API 기준 해시:

- `10_API-Design.md`: `13d0453f574dbd60cb598a3502b9be680640f897ce9429ec6ba10cf9c5ce336b`
- 백엔드 가이드 다운로드본: `0a241ef20cb96847705a51fc5a70fc126df472af09b5eb00b0ac384e697b0fa0`

새 API 재갱신 핵심:

- Data Model 기준 enum 목록이 API 문서에 명시됐다.
- `ProjectRoomResponse.paymentStatus`, `ProjectRoomResponse.status`, `ResourceResponse.status`, `summaryStatus`, `aiDocumentStatus`가 공통 enum 타입 기준으로 정리됐다.
- 상태 row가 없어서 판단할 수 없는 값은 API 전용 enum 대신 `null` 또는 필드 생략으로 표현한다.
- API 전용 상태값이 꼭 필요하면 `Api` 접두사를 붙이고 DB에는 저장하지 않는다.

## 작업 원칙

- 6/25 문서 묶음과 12:16 KST API 재갱신본을 기준으로 구현한다.
- 최종 기획문서와 `09_Data-Model.md`와 충돌하면 기획/DB 기준을 우선해서 `WORK_HANDOFF.md`에 남긴다.
- 한 번에 한 기능 또는 한 PR만 처리한다.
- 이미 열려 있는 PR과 섞이지 않게 브랜치와 변경 범위를 먼저 확인한다.
- 작업 끝에는 로컬 검증과 `WORK_HANDOFF.md` 갱신을 한다.
- push/PR을 만들었다면 GitHub Actions CI 통과까지 확인한다.
- 개발 가이드는 참고 문서가 아니라 ArchUnit/CI 통과 기준으로 취급한다.
- 기존 `V1` migration은 수정하지 않고, 필요한 변경은 새 migration으로 분리한다.

## 6/25 기준으로 다시 확인할 것

| 우선순위 | 작업 | 설명 |
|---|---|---|
| 0 | API 기준 재갱신 | `10_API-Design (1).md`를 canonical API로 반영하고 해시 기록 |
| 1 | ArchUnit 기준선 격상 | #22의 도메인 의존 테스트와 #38 entity boundary guard를 전체 작업 기준으로 끌어올림 |
| 2 | #19~#22 API 재검토 | 새 API/DB/ArchUnit 기준과 develop 대상 PR 차이 확인 |
| 3 | #23~#39 foundation stack 재검토 | 권한, 인증, 자료, 에이전트, Entity/Flyway, CI 보강 PR 차이 확인 |
| 4 | Resource stack 재검토 | 상태 enum, 파일 업로드, 삭제 retry/dead-letter, storage usage 기준 정리 |
| 5 | Agent stack 재검토 | `agent_jobs`, `agent_suggestions`, `ai_documents`, 모델 호출 로그 기준 재확인 |
| 6 | User/settings stack 재검토 | Google-only 사용자 프로필/설정/동의 API와 enum 기준 확인 |
| 7 | Flyway/FK 정합성 재검토 | 기존 V1 수정 이력 제거, V2 이후 migration 분리 원칙 확인 |
| 8 | 테스트/CI 보정 | ArchUnit, Entity/Flyway 검사, stacked PR 로컬 검증과 CI 재확인 |

## 바로 이어서 할 추천 순서

1. API 재갱신 PR을 먼저 만든다.
2. ArchUnit/refactor 기준선을 반영한다.
3. #19~#81 PR을 base 순서대로 새 기준에 맞춰 점검한다.
4. 바로 고칠 수 있는 endpoint, DTO, enum, 권한 검사, Request-to-Command 차이를 작은 보정 PR로 나눈다.
5. 코드 보정 PR마다 `./gradlew test --tests '*ArchitectureTest'`, `./gradlew compileTestJava`, `./gradlew cleanTest test`, `git diff --check`를 확인한다.

## 추상화 기준 재시작 조건

2026-06-25 18:35 KST에 다운로드본 백엔드 가이드의 추상화 기준이 현재 6/25 기준 문서에 반영됐다.
이 기준은 기존 CI/CLEAN 상태를 무효화하지 않는다.
다만 merge 전에 설계 영향이 있는 PR만 선별 재검토한다.
다운로드본 중 현재 `09_Data-Model.md`, `10_API-Design.md`와 충돌하는 표현은 덮어쓰지 않고, Service 인터페이스화 기준만 반영했다.

확정 기준:

- 다른 도메인이 의존하는 `*PublicService`는 인터페이스로 둔다.
- 구현체는 `*PublicServiceImpl` 이름을 쓴다.
- 다른 도메인은 구현체가 아니라 `*PublicService` 인터페이스를 주입받는다.
- Storage, OAuth/OIDC, AI model, agent 실행, agent dispatch, queue, S3/object storage, calendar provider, 외부 API는 외부 연동 포트로 보고 인터페이스화한다.
- 같은 도메인 Controller만 호출하는 단순 CRUD Service는 구체 클래스로 유지한다.
- 구현체가 하나뿐이고 외부 연동도 도메인 간 계약도 아닌 helper Service는 구체 클래스로 유지한다.

재시작하면 먼저 볼 대상:

| 우선순위 | 대상 | 이유 |
|---|---|---|
| 1 | `ProjectMembershipPublicService`, `ResourcePublicService`, `StoragePublicService`, `StorageUsagePublicService`, `UserPublicService`, `TaskPublicService`, `WbsItemPublicService` | 다른 도메인에서 호출하는 공개 계약 |
| 2 | Storage/S3/download-url/upload/delete retry stack | 외부 저장소 구현 교체 가능성 |
| 3 | OAuth/OIDC auth stack | Google-only라도 외부 연동 client 경계가 필요함 |
| 4 | Agent dispatch/queue/execution/model call stack | 실행 방식과 provider 교체 가능성 |
| 5 | 단순 CRUD Service | 기본 유지. 다른 도메인 호출이 생긴 경우만 재검토 |

추천 재시작 목표:

```text
PublicService와 외부 연동 포트의 인터페이스화 보정 PR을 만든다.
먼저 develop 또는 가장 이른 foundation base에서 ProjectMembershipPublicService 계열을 인터페이스/구현체로 분리하고,
컴파일/ArchUnit/테스트를 통과시킨 뒤 후속 PR stack에 필요한 병합 순서를 WORK_HANDOFF.md에 기록한다.
```

## 새 기준 문서가 다시 오면 하는 일

사용자가 "API 명세 완성본이 왔다"라고 말하면 구현을 멈추고 아래를 먼저 한다.

1. 새 기준 문서 파일 위치를 확인한다.
2. 기존 6/25 문서와 차이를 비교한다.
3. 기준 문서와 로컬 스킬의 API 경로를 갱신한다.
4. 현재 구현된 API와 새 기준의 차이를 표로 만든다.
5. 차이 보정 작업을 PR 단위로 나눈다.
6. 기존 구현을 무조건 버리지 않고, 필요한 부분만 수정한다.

## 복붙 프롬프트

```text
bubli-backend-workflow로 진행해줘.
2026-06-25 최신 기준 보정 모드야.

작업 위치:
/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend

먼저 아래 문서를 읽어줘.
1. docs/00_BACKEND_START_HERE.md
2. docs/WORK_HANDOFF.md
3. docs/CODEX_BACKEND_WORKFLOW.md
4. docs/CURRENT_API_BASELINE_WORK.md

2026-06-25 기준 문서 묶음을 기준으로
WORK_HANDOFF.md의 다음 우선순위 보정 작업 1개만 처리해줘.

조건:
- 한 번에 한 기능 또는 한 PR만 처리
- 현재 git 변경사항을 먼저 확인
- 이미 작업 중인 변경분이 있으면 섞지 말고 정리 방법부터 보고
- 새 기준 문서가 다시 오면 별도 보정 작업으로 처리한다는 전제로 진행
- 작업 후 WORK_HANDOFF.md 갱신

검증:
./gradlew test --tests '*ArchitectureTest'
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check

push/PR까지 했다면 GitHub Actions CI 통과도 확인해줘.
```

## 금지

- 6/24 문서를 기준으로 새 작업하지 않는다.
- 여러 기능을 한 브랜치에 섞지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- `BaseTimeEntity`나 global 공통 엔티티를 만들지 않는다.
- Tauri `local_*` 테이블을 서버 JPA 엔티티로 만들지 않는다.
- 에이전트가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하게 만들지 않는다.
