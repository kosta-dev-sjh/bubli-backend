# 현재 API 기준 백엔드 작업 계획

Last checked: 2026-06-24 20:33:25 KST

현재 `10_API-Design.md`는 최종 수정본이 아닐 수 있다.
그래도 백엔드 작업을 멈추지 않는다.
지금은 현재 API Design을 작업 기준선으로 삼아 백엔드 뼈대와 기본 API를 구현한다.
나중에 수정본이 오면 기준 문서와 스킬을 갱신하고, 차이 나는 API를 별도 수정 작업으로 처리한다.

## 작업 원칙

- 현재 API Design을 기준으로 구현한다.
- 최종 기획문서와 데이터 딕셔너리와 충돌하면 기획/DB 기준을 우선해서 `WORK_HANDOFF.md`에 남긴다.
- 한 번에 한 기능 또는 한 PR만 처리한다.
- 이미 열려 있는 PR과 섞이지 않게 브랜치와 변경 범위를 먼저 확인한다.
- 작업 끝에는 로컬 검증과 `WORK_HANDOFF.md` 갱신을 한다.
- push/PR을 만들었다면 GitHub Actions CI 통과까지 확인한다.

## 수정본 API가 오기 전에도 구현할 수 있는 것

| 우선순위 | 작업 | 설명 |
|---|---|---|
| 0 | 현재 변경분 정리 | 카드 2 권한 검사 서비스 분리 작업을 커밋/푸시/PR로 정리 |
| 1 | 인증 기반 정리 | Google-only 방향으로 auth 문서, TODO, signup 흔적 정리 |
| 2 | ProjectRoom 공통 권한 기반 | `RoomAccessService`처럼 모든 room API가 쓸 권한 검사 기반 확정 |
| 3 | work.task 기본 뼈대 | 현재 API 기준으로 Task Entity/Repository/Service/Controller/테스트 작성 |
| 4 | work.wbs 기본 뼈대 | 현재 API 기준으로 WBS Entity/Repository/Service/Controller/테스트 작성 |
| 5 | chat 기본 뼈대 | 현재 API 기준으로 채팅방/메시지 기본 API와 Repository/Service 작성 |
| 6 | resource 기본 뼈대 | 자료 업로드 전 단계의 Resource/metadata Repository/Service 기반 작성 |
| 7 | agent 저장 기반 | `AgentJob`, `AgentSuggestion`, `AiDocument` Repository/Service 뼈대 작성 |
| 8 | Entity/Flyway 정합성 | DB 매핑 오류와 누락 제약 확인 |
| 9 | Testcontainers/CI 보강 | 기능 PR이 같은 방식으로 통과하도록 테스트 기반 보강 |

## 바로 이어서 할 추천 순서

1. 현재 변경분을 카드 2 PR로 정리한다.
2. 카드 3 인증 기반 정리를 한다.
3. 현재 API 기준으로 `work.task` 기본 API를 만든다.
4. 현재 API 기준으로 `work.wbs` 기본 API를 만든다.
5. 채팅, 자료, 에이전트 저장 기반 순서로 이어간다.

## 수정본 API가 오면 하는 일

사용자가 "API 명세 완성본이 왔다"라고 말하면 구현을 멈추고 아래를 먼저 한다.

1. 새 API 명세 파일 위치를 확인한다.
2. 기존 `10_API-Design.md`와 차이를 비교한다.
3. 기준 문서와 로컬 스킬의 API 경로를 갱신한다.
4. 현재 구현된 API와 새 명세의 차이를 표로 만든다.
5. 차이 보정 작업을 PR 단위로 나눈다.
6. 기존 구현을 무조건 버리지 않고, 필요한 부분만 수정한다.

## 복붙 프롬프트

```text
bubli-backend-workflow로 진행해줘.
현재 API 기준 작업 모드야.

작업 위치:
/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend

먼저 아래 문서를 읽어줘.
1. docs/00_BACKEND_START_HERE.md
2. docs/WORK_HANDOFF.md
3. docs/CODEX_BACKEND_WORKFLOW.md
4. docs/CURRENT_API_BASELINE_WORK.md

현재 10_API-Design.md를 작업 기준선으로 삼아
WORK_HANDOFF.md의 다음 우선순위 작업 1개만 처리해줘.

조건:
- 한 번에 한 기능 또는 한 PR만 처리
- 현재 git 변경사항을 먼저 확인
- 이미 작업 중인 변경분이 있으면 섞지 말고 정리 방법부터 보고
- 최종 API 수정본이 나중에 오면 별도 보정 작업으로 처리한다는 전제로 진행
- 작업 후 WORK_HANDOFF.md 갱신

검증:
./gradlew compileTestJava
./gradlew cleanTest test
git diff --check

push/PR까지 했다면 GitHub Actions CI 통과도 확인해줘.
```

## 금지

- API 수정본을 기다린다는 이유로 백엔드 작업을 멈추지 않는다.
- 여러 기능을 한 브랜치에 섞지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- `BaseTimeEntity`나 global 공통 엔티티를 만들지 않는다.
- Tauri `local_*` 테이블을 서버 JPA 엔티티로 만들지 않는다.
- 에이전트가 `tasks`, `wbs_items`, `schedules`, `memos`를 직접 확정 저장하게 만들지 않는다.
