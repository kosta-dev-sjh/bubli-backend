# Flyway 마이그레이션 가이드

## 1. Flyway란

Flyway는 데이터베이스 스키마 버전 관리 도구다. SQL 마이그레이션 파일을 버전 순서대로 실행하고, 각 실행 이력을 `flyway_schema_history` 테이블에 기록한다. 애플리케이션 기동 시 미적용 마이그레이션을 자동으로 감지해 순서대로 실행하므로, 개발·스테이징·운영 환경의 스키마를 코드와 함께 일관되게 관리할 수 있다.

---

## 2. 마이그레이션 파일 위치와 네이밍 규칙

### 파일 위치

```
src/main/resources/db/migration/
```

### 네이밍 규칙

```
V{버전}__{설명}.sql
```

| 구성 요소 | 규칙 | 예시 |
|-----------|------|------|
| 접두사 | 대문자 `V` 고정 | `V` |
| 버전 | 양의 정수, 중복 불가 | `1`, `2`, `3` |
| 구분자 | 언더스코어 **2개** (`__`) | `__` |
| 설명 | 소문자 + 언더스코어, 변경 내용 요약 | `init_schema` |
| 확장자 | `.sql` | `.sql` |

**올바른 예시**
```
V4__add_user_profile_image.sql
V5__resource_tag_table.sql
```

**잘못된 예시**
```
V4_add_user_profile_image.sql   ← 구분자가 _ 1개
v4__add_user_profile_image.sql  ← 소문자 v
V04__add_user_profile_image.sql ← 버전에 0 패딩 (혼용 시 정렬 오류)
```

---

## 3. 현재 마이그레이션 파일 목록

### V1\_\_init\_schema.sql

전체 스키마 초기 생성. `pgvector` extension 활성화 후 모든 테이블을 생성한다.

| 도메인 | 테이블 |
|--------|--------|
| 사용자 | `users`, `user_sessions`, `user_preferences`, `user_notification_preferences`, `user_privacy_consents` |
| 친구 | `friend_requests`, `friendships` |
| 프로젝트룸 | `project_rooms`, `room_members`, `invitations`, `project_room_events` |
| 자료 | `resources`, `resource_files`, `resource_versions`, `resource_summaries`, `resource_embeddings`, `resource_comments`, `resource_relations`, `resource_storage_delete_requests` |
| AI / 에이전트 | `ai_documents`, `agent_jobs`, `agent_job_events`, `agent_dispatch_outbox`, `agent_model_call_logs`, `agent_suggestions`, `room_memory_summaries`, `daily_summaries` |
| 업무 | `wbs_items`, `tasks`, `schedules`, `memos`, `time_logs`, `activity_logs` |
| 채팅 / 음성 | `chat_rooms`, `chat_room_members`, `chat_messages`, `notifications`, `voice_rooms`, `voice_participants` |
| 스토리지 | `storage_usage` |
| 위젯 | `widget_context_settings`, `widget_bubble_settings`, `widget_item_states`, `widget_daily_summaries` |

인덱스: `idx_resource_storage_delete_requests_status_updated_at`

---

### V2\_\_chat\_message\_client\_id\_scope.sql

채팅 메시지 클라이언트 ID 유니크 범위 변경.

- `chat_messages.client_message_id` 전역 UNIQUE 제약 삭제
- `(chat_room_id, client_message_id)` 복합 UNIQUE 제약 추가
  → 같은 클라이언트 ID를 다른 채팅방에서 재사용 가능하도록 범위를 채팅방 내로 한정
- `chat_room_members`에 `last_read_sequence BIGINT` 컬럼 추가

---

### V3\_\_core\_domain\_fks\_and\_lookup\_indexes.sql

외래키 제약 조건과 조회 인덱스 일괄 추가.

**외래키**: 모든 도메인 테이블에 참조 무결성 제약 추가 (users, project_rooms, resources, agent_jobs 등 전 테이블)

**인덱스**: 주요 조회 패턴 기반

| 인덱스 | 용도 |
|--------|------|
| `idx_room_members_user_status` | 사용자별 룸 멤버십 조회 |
| `idx_resources_owner_status` | 내 자료 목록 조회 |
| `idx_resources_room_status` | 룸 자료 목록 조회 |
| `idx_resource_comments_resource_created` | 자료 댓글 목록 조회 |
| `idx_agent_jobs_requested_status` | 사용자별 에이전트 작업 조회 |
| `idx_tasks_owner_status_due` | 내 할일 목록 조회 |
| `idx_schedules_owner_starts` | 내 일정 조회 |
| `idx_chat_messages_chat_room_created` | 채팅 메시지 커서 페이징 |
| (외 다수) | |

---

## 4. 새 마이그레이션 파일 추가하는 방법

1. **현재 최신 버전 확인**

   ```
   V3__core_domain_fks_and_lookup_indexes.sql  ← 현재 최신 (V3)
   ```

2. **다음 버전 번호로 파일 생성**

   ```
   src/main/resources/db/migration/V4__{변경_내용_요약}.sql
   ```

3. **SQL 작성**

   ```sql
   -- 예시: 새 테이블 추가
   CREATE TABLE example_table (
       id UUID PRIMARY KEY,
       name VARCHAR(100) NOT NULL,
       created_at TIMESTAMPTZ NOT NULL
   );
   ```

4. **애플리케이션 기동 시 자동 적용** — 별도 명령 불필요

---

## 5. 주의사항

### 기존 파일 절대 수정 금지

Flyway는 이미 실행된 파일의 체크섬을 저장한다. 기존 파일을 수정하면 체크섬 불일치 오류(`FlywayException: Validate failed`)가 발생해 애플리케이션이 기동되지 않는다.

**기존 스키마 변경은 반드시 새 버전 파일로 작성한다.**

```sql
-- 잘못된 방법: V1__init_schema.sql 직접 수정 ← 절대 금지
-- 올바른 방법:
-- V4__add_column_to_existing_table.sql
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
```

### 버전 번호 중복 금지

같은 버전 번호가 두 개 이상 존재하면 Flyway가 기동 시 오류를 낸다. PR 머지 전 팀원과 버전 번호를 조율한다.

### 롤백 불가 구조 인식

Flyway Community Edition은 자동 롤백을 지원하지 않는다. 마이그레이션 실패 시 수동으로 DB 상태를 복구해야 한다. DDL은 트랜잭션 안에서 실행되므로 한 파일 내 실패는 해당 파일 전체가 롤백된다.

### 운영 환경 배포 전 반드시 검증

로컬 → 스테이징 순으로 마이그레이션을 검증한 뒤 운영에 반영한다. 대용량 테이블 컬럼 추가나 인덱스 생성은 잠금(lock)을 유발할 수 있으므로 `CONCURRENTLY` 옵션 등을 고려한다.

---

## 6. 로컬 개발 환경에서 마이그레이션 초기화 방법

### DB 스키마 전체 초기화 (주의: 데이터 전부 삭제)

```bash
# 1. Docker DB 컨테이너 재시작
docker compose down -v
docker compose up -d

# 2. 애플리케이션 기동 시 Flyway가 V1부터 순서대로 자동 적용
./gradlew bootRun
```

### Flyway 이력만 초기화 (테이블 유지, 히스토리만 리셋)

```sql
-- flyway_schema_history 테이블 직접 삭제 후 재기동
DROP TABLE flyway_schema_history;
```

재기동 시 Flyway가 `flyway_schema_history`가 없음을 감지하고 전체 마이그레이션을 다시 실행한다. 단, 이미 테이블이 존재하면 `CREATE TABLE` 구문이 충돌하므로, 테이블도 함께 드롭하거나 `spring.flyway.baseline-on-migrate=true` 옵션을 사용한다.

### 특정 버전부터 재실행

```bash
# application-local.yml 또는 환경변수로 설정
spring:
  flyway:
    target: 2   # V2까지만 적용 (V3 이후는 보류)
```

이후 `target` 설정을 제거하면 다음 기동 시 나머지 버전이 적용된다.
