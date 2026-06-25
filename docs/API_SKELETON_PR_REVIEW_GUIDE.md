# API Skeleton PR Review Guide

Last checked: 2026-06-25 16:45 KST

이 문서는 현재 열린 백엔드 PR을 팀원이 어떻게 봐야 하는지 정리한 기준이다.
현재 PR들은 완성 기능 묶음이 아니라, 팀원이 같은 구조로 이어서 개발할 수 있게 만든 기본 API 골격이다.

PR별 상태를 표로 필터링하려면 `docs/API_SKELETON_PR_MATRIX.csv`를 함께 본다.

## 현재 PR의 성격

현재 PR에서 말하는 기본 API 골격은 아래 범위다.

- Controller endpoint
- Request/Response DTO
- Service의 기본 흐름
- Repository와 Entity 연결
- Flyway 정합성 보강
- 기본 단위 테스트와 통합 테스트
- `docs/http` 호출 예시
- ArchUnit 기반 계층/도메인 경계 검사

이 범위는 빈 껍데기가 아니다.
컴파일되고 테스트가 통과하는 기본 흐름이다.

하지만 완성 기능도 아니다.
아래 작업은 각 담당자가 이어서 구현하거나 보정해야 한다.

- 실제 OAuth, S3, Redis, AI/RAG, WebSocket 운영 연동
- 화면에서 필요한 세부 응답 필드 조정
- 복잡한 권한 예외 케이스
- 운영 정책, 알림, 재시도, 모니터링 상세
- 최종 API 수정본 반영
- 프론트 연동 중 발견되는 요청/응답 보정

한마디로, 지금 PR은 강사님 예제처럼 Controller에서 DB까지 흐름이 이어지는 토대다.
팀원은 이 토대 위에서 자기 담당 기능의 실제 정책과 예외를 붙이면 된다.

## 팀원이 PR을 보는 순서

처음부터 열린 PR 전체를 다 보지 않는다.
아래 순서로 본다.

1. `develop` 대상이고 CI가 통과한 PR부터 본다.
2. `base`가 feature branch인 stacked PR은 앞 PR이 들어간 뒤 순서대로 본다.
3. `docs/API_SKELETON_PR_MATRIX.csv`에서 `type`, `status`, `review_priority`, `team_action`을 확인한다.
4. `WORK_HANDOFF.md`의 열린 PR 표에서 head, base, CI 상태를 확인한다.
5. PR 본문의 `현재 API 기준`, `최종 API 수정본 수신 시 보정 가능 부분`, `검증 결과`를 먼저 읽는다.
6. 담당 도메인의 Controller, Service, Test, `.http` 예시만 먼저 확인한다.

## 지금 바로 병합 검토 가능한 PR

2026-06-25 16:45 KST 기준으로 바로 `develop` 병합 검토가 쉬운 PR은 아래다.

| PR | 기준 | 상태 | 판단 |
|---|---|---|---|
| #47 | `PATCH /api/me` 기본 API | CI 통과, review required | 팀 리뷰 후 병합 검토 가능 |
| #83 | time-log 기본 API | CI 통과, review required | 팀 리뷰 후 병합 검토 가능 |
| #84 | Terraform RDS 백업/삭제 보호 | CI 통과, review required | 인프라 담당자 리뷰 후 병합 검토 가능 |
| #85 | project room management 기본 API | CI 통과, review required | 팀 리뷰 후 병합 검토 가능 |

`review required`는 충돌이나 테스트 실패가 아니다.
리뷰 승인 규칙 때문에 막힌 상태로 본다.

## Stacked PR을 보는 방법

stacked PR은 `develop`이 아니라 앞 PR을 base로 둔 PR이다.
GitHub Actions check가 비어 있을 수 있다.
이 경우에는 PR 본문과 `WORK_HANDOFF.md`에 적힌 로컬 검증 결과를 기준으로 본다.

stacked PR을 볼 때는 아래를 확인한다.

- base PR이 먼저 들어갈 수 있는 상태인지
- 현재 PR의 head가 `WORK_HANDOFF.md`와 같은지
- `./gradlew test --tests '*ArchitectureTest'`가 통과했는지
- `./gradlew compileTestJava`가 통과했는지
- `./gradlew cleanTest test`가 통과했는지
- `git diff --check`가 통과했는지
- PR 본문에 stacked PR이라 checks가 없는 이유가 적혀 있는지

stacked PR은 순서를 바꿔 merge하면 충돌이 날 수 있다.
앞 PR이 merge된 뒤 최신 `develop` 위로 다시 올릴 수 있으면, 별도 `codex/develop-*` PR로 재구성한다.

## 팀원이 이어서 할 수 있는 일

각 담당자는 자기 도메인 PR을 볼 때 아래처럼 이어서 작업하면 된다.

| 담당 작업 | 이어서 볼 부분 |
|---|---|
| 화면 연동 | Controller path, Request/Response DTO, `docs/http` 예시 |
| 서비스 정책 | Service TODO성 기본 흐름, 예외 코드, 권한 검사 위치 |
| DB 보정 | Entity, Repository, Flyway migration, schema test |
| 테스트 보강 | ServiceTest, IntegrationTest, ArchitectureTest |
| 최종 API 반영 | PR 본문의 보정 가능 부분과 `10_API-Design.md` 변경점 |

담당자는 PR을 그대로 끝난 기능으로 보지 않는다.
자기 화면과 정책에 맞게 응답 필드, 예외, 권한, 연동 처리를 추가한다.

## Merge 전에 확인할 것

merge 전에 아래만 빠르게 확인한다.

1. PR이 draft가 아닌지 확인한다.
2. base가 `develop`인지, stacked feature branch인지 확인한다.
3. `develop` 대상이면 GitHub Actions CI가 통과했는지 확인한다.
4. stacked PR이면 앞 PR이 먼저 들어갔는지 확인한다.
5. PR 본문의 로컬 검증 결과가 최신 head와 맞는지 확인한다.
6. 다른 도메인의 Repository나 Entity를 직접 참조하지 않는지 확인한다.
7. Entity를 API 응답으로 직접 반환하지 않는지 확인한다.
8. `V1` Flyway를 수정하지 않았는지 확인한다.
9. secret, API key, `application-secret.yml`이 포함되지 않았는지 확인한다.

## 코드가 많이 보여도 완성 기능으로 보지 않는 이유

Spring 백엔드에서는 기본 API 하나만 만들어도 Controller, DTO, Service, Repository, Entity, Test가 함께 생긴다.
그래서 코드 줄 수는 많아 보일 수 있다.

하지만 현재 PR의 Service는 보통 아래 수준이다.

- 현재 사용자 확인
- 프로젝트룸 멤버 권한 확인
- 기본 조회
- 기본 저장 또는 upsert
- DTO 변환
- 최소 예외 처리

아래 수준까지 끝낸 것은 아니다.

- 외부 서비스 실운영 연동
- 복잡한 업무 정책
- 화면별 세밀한 UX 응답
- 장애 복구와 운영 알림
- 실제 에이전트 모델 실행

그래서 팀원은 이 PR을 받더라도 자기 담당 기능을 계속 개발해야 한다.

## PR 본문에서 꼭 봐야 할 문장

각 PR 본문에는 아래 정보가 있어야 한다.

- 작업 내용
- 변경 파일 요약
- 현재 API 기준으로 구현했다는 점
- 최종 API 수정본이 오면 보정될 수 있는 부분
- 로컬 검증 결과
- GitHub Actions CI 결과

이 중 하나가 없으면 merge 전에 보강 요청한다.

## 문제가 있으면 이렇게 처리한다

| 상황 | 처리 |
|---|---|
| CI 실패 | 원인을 보고 한 번 수정 후 다시 push |
| 같은 실패 반복 | 해당 PR에서 멈추고 보고 |
| 충돌 발생 | 임의 merge하지 않고 충돌 파일과 기준 branch를 보고 |
| API 최종본 변경 | 기존 PR을 버리지 말고 차이만 보정 |
| 기능 범위가 커짐 | 새 PR로 쪼갠다 |
