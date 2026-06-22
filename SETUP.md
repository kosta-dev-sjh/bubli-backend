# 프로젝트 초기 세팅 안내

이 zip은 `build.gradle`, `settings.gradle`, 패키지 구조, 기본 설정 파일까지 포함하고 있지만
**Gradle Wrapper(`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)는 바이너리 파일이라 포함되어 있지 않습니다.**

## 처음 받은 사람이 해야 할 일 (1회만)

1. 이 폴더를 압축 해제한다.
2. IntelliJ에서 `build.gradle`이 있는 폴더를 **Open**으로 연다. (Import 아님, 그냥 Open)
3. IntelliJ가 자동으로 Gradle 프로젝트로 인식하고 동기화를 시작한다.
4. 동기화가 끝나면 터미널에서 아래 명령으로 Gradle Wrapper를 생성한다.

```bash
gradle wrapper --gradle-version 8.11
```

   (로컬에 Gradle이 없으면 `brew install gradle`로 먼저 설치하거나, IntelliJ의 Gradle 도구 창에서 "Generate Wrapper" 메뉴를 사용해도 됩니다.)

5. 생성된 `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`를 **그대로 커밋**한다. (이 파일들은 `.gitignore`에 안 걸려 있음 — wrapper jar는 예외적으로 커밋하는 게 Gradle 공식 권장 방식)
6. 이후 팀원들은 `git clone` 후 바로 `./gradlew bootRun`으로 실행 가능해진다.

## DB / Redis 로컬 준비

`application-local.yml` 기준으로 PostgreSQL이 `localhost:5432/bubli`, Redis가 `localhost:6379`에 떠 있어야 합니다.

```bash
docker compose up -d
```

이 한 줄이면 PostgreSQL(pgvector 포함) + Redis가 함께 뜹니다.

```bash
docker compose down      # 종료
docker compose down -v   # 종료 + 데이터 삭제 (DB 초기화)
```

## 민감정보 설정

`src/main/resources/application-secret.yml.example`을 복사해서 `application-secret.yml`로 만들고 실제 값을 채워 넣으세요. 이 파일은 `.gitignore`에 등록되어 있어 커밋되지 않습니다.

```bash
cp src/main/resources/application-secret.yml.example src/main/resources/application-secret.yml
```
