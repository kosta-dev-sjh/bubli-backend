# 로컬 개발용 공용 seed 데이터

이 문서는 팀원이 같은 화면을 보면서 API와 프론트 작업을 확인하기 위한 로컬 Docker DB seed 절차다.

## 성격

- 백엔드 테스트 코드에 원래 들어 있던 fixture가 아니다.
- 로컬 Docker Postgres에 넣는 개발용 데이터다.
- 운영 DB나 배포 DB에 넣으면 안 된다.
- 같은 파일을 여러 번 실행해도 정해진 demo ID만 지우고 다시 넣는다.

## 들어가는 데이터

대표 프로젝트룸:

- `22222222-2222-4222-8222-222222222222`
- 이름: `브랜드 상세페이지`

사용자:

- `Maren` / `maren_local`
- `민지` / `minji_local`

포함 범위:

- 사용자, 친구 관계
- 프로젝트룸, 멤버
- WBS 10개
- TODO 6개
- 일정 3개
- 개인 자료 1개, 프로젝트룸 자료 2개
- 프로젝트룸 대화, 1:1 대화
- 보이스룸 참여자
- 에이전트 후보, 생성 문서
- 알림, 위젯 상태, 위젯 사용량 요약

## 실행 방법

백엔드 repo 루트에서 실행한다.

```bash
docker compose up -d postgres redis
docker exec -i bubli-postgres psql -U bubli -d bubli < scripts/dev/seed-local-workboard.sql
```

백엔드를 실행한다.

```bash
./gradlew bootRun
```

프론트는 로컬 백엔드를 보게 둔다.

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

프론트에서 확인할 대표 주소:

```text
/app
/app/project-rooms/22222222-2222-4222-8222-222222222222
/app/project-rooms/22222222-2222-4222-8222-222222222222/work
/app/project-rooms/22222222-2222-4222-8222-222222222222/resources
/app/chat?roomId=22222222-2222-4222-8222-222222222222
/app/calendar?roomId=22222222-2222-4222-8222-222222222222
/app/agent
```

## 로그인 주의

이 seed는 DB 데이터만 넣는다. 구글 OAuth 토큰을 만들지는 않는다.

실제 로그인 확인은 백엔드 Google OAuth 설정이 연결된 환경에서 한다. 로컬에서 화면만 빠르게 볼 때는 프론트의 개발용 인증 세션 생성 절차를 따로 써야 한다. 이 절차는 프론트 repo에서 관리한다.

## seed를 다시 넣을 때

같은 명령을 다시 실행하면 된다.

```bash
docker exec -i bubli-postgres psql -U bubli -d bubli < scripts/dev/seed-local-workboard.sql
```

이때 demo 사용자의 기존 로컬 세션은 지워질 수 있다. 다시 로그인하거나 개발용 인증 세션을 다시 넣으면 된다.
