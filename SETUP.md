# Local Setup

이 문서는 `RDS`나 `S3` 없이, EC2 안에서만 도는 로컬 구성을 기준으로 합니다.

## 1. 시작

```bash
docker compose up -d
```

이 명령으로 다음 서비스가 올라갑니다.

- `postgres` - PostgreSQL + pgvector
- `redis` - Redis
- `backend` - Spring Boot backend

backend는 기본적으로 `local` 프로필로 실행되며, 파일 저장은 `./local-storage`에 저장됩니다.

## 2. 확인

```bash
curl http://localhost:8080/actuator/health
```

기본 API 서버가 올라왔는지 먼저 확인하고, 필요하면 Swagger나 Postman으로 이어서 검증하면 됩니다.

## 3. AI까지 같이 쓰고 싶을 때

Bedrock 기반 AI 기능이 필요하면 `SPRING_PROFILES_ACTIVE=local,ai`로 실행하세요.

예시:

```bash
$env:SPRING_PROFILES_ACTIVE = "local,ai"
docker compose up -d
```

이 경우에도 `RDS`와 `S3`는 쓰지 않고, DB와 파일 저장은 로컬 컨테이너와 디스크를 사용합니다.

## 4. 종료

```bash
docker compose down
docker compose down -v
```

- `down`은 컨테이너만 종료합니다.
- `down -v`는 DB 볼륨까지 삭제해서 완전 초기화합니다.

## 5. 선택 사항

`application-secret.yml.example`은 실제 Google OAuth, LiveKit 값을 넣고 싶을 때만 복사해서 사용하세요.
로컬 개발만 할 거면 기본값으로도 시작할 수 있습니다.
