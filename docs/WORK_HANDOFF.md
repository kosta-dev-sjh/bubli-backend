# Bubli Backend Work Handoff

Last checked: 2026-07-05 KST

이 문서는 백엔드 현재 상태를 이어받기 위한 인수인계 문서다.
작업이 끝날 때마다 이 문서의 PR 상태, 확인 결과, 다음 작업을 갱신한다.

## 현재 기준

| 항목 | 값 |
|---|---|
| 로컬 레포 | `/Users/maren/EDU/Final Project/04_개발_작업공간/repos/bubli-backend` |
| 현재 확인 브랜치 | `codex/user-settings-upsert-guard-20260705` |
| 원격 기준 브랜치 | `develop` |
| 시작 문서 | `docs/00_BACKEND_START_HERE.md` |
| API 기준 | `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md` |
| DB 기준 | `/Users/maren/EDU/Final Project/04_개발_작업공간/DB_팀검토_2026-06-23/Bubli_DB_검토보드/09_데이터딕셔너리_회의반영_2026-06-24.html` |
| 백엔드 규칙 | `docs/Bubli_백엔드_개발_가이드_2026-06-24.md` |
| 현재 API 기준 작업 계획 | `docs/CURRENT_API_BASELINE_WORK.md` |
| API 확정 전 기반 작업 카드 | `docs/API_PRE_FINAL_TASKS.md` |

## 현재 작업 모드

상태: 현재 `10_API-Design.md`를 작업 기준선으로 삼아 구현한다.

지금은 API 수정본을 기다리며 멈추지 않는다.
현재 API 기준으로 백엔드 뼈대와 기본 API를 만들고, 사용자가 새 API 명세 완성본을 주면 `API 명세 완성본 수신 절차`로 전환해 차이 보정 작업을 한다.

현재 자동화 목표에서는 #22 `feature/schedule-basic-api`를 구현 기준선으로 인정한다.
따라서 `develop`에 아직 없는 `ScheduleService`가 있어도 멈추지 않고, #22 위에 이어지는 foundation 작업은 stacked PR 또는 현재 브랜치 기준 커밋으로 정리한다.
stacked PR이라 GitHub Actions가 실행되지 않으면 로컬 검증 결과를 남기고, #22 merge 후 `develop` 기준 CI를 다시 확인한다.

현재 컴파일 기준:

- `./gradlew compileTestJava` 통과 (2026-06-25 00:49 KST)
- `./gradlew cleanTest test` 통과 (2026-06-25 00:49 KST)
- `git diff --check` 통과 (2026-06-25 00:49 KST)
- `./gradlew compileTestJava` 통과 (2026-06-25 02:40 KST, resource related API)
- `./gradlew cleanTest test` 통과 (2026-06-25 02:40 KST, resource related API)
- `git diff --check` 통과 (2026-06-25 02:40 KST, resource related API)
- `./gradlew compileTestJava` 통과 (2026-06-25 03:48 KST, storage usage API)
- `./gradlew cleanTest test` 통과 (2026-06-25 03:48 KST, storage usage API)
- `git diff --check` 통과 (2026-06-25 03:48 KST, storage usage API)
- `./gradlew compileTestJava` 통과 (2026-06-25 00:42 KST)
- `./gradlew cleanTest test` 통과 (2026-06-25 00:43 KST)
- `git diff --check` 통과 (2026-06-25 00:44 KST)
- `./gradlew compileTestJava` 통과 (2026-06-25 02:44 KST, agent job status API)
- `./gradlew cleanTest test` 통과 (2026-06-25 02:44 KST, agent job status API)
- `git diff --check` 통과 (2026-06-25 02:44 KST, agent job status API)
- 엔티티 44개, Repository 4개, Controller 4개, Service 5개 확인
- 현재 API 기준 세부 작업 지시는 `docs/CURRENT_API_BASELINE_WORK.md`를 기준으로 나눈다.

## 최근 완료 작업

### 사용자 알림/동의 설정 upsert 저장 안정화

처리 시각: 2026-07-05 KST

변경 내용:

- `user_notification_preferences`, `user_privacy_consents`는 사용자와 설정 타입을 primary key로 쓰므로 첫 저장 요청이 동시에 들어오면 생성 충돌이 날 수 있다.
- 알림 설정 저장은 `INSERT ... ON CONFLICT DO UPDATE`로 바꿔 기존 row가 있으면 바로 갱신하게 했다.
- 개인정보 동의 저장도 같은 upsert 경로로 바꾸고, 생성/수정 시 `updated_at`을 DB 기준 현재 시각으로 갱신한다.
- 저장 후 현재 DB row를 다시 읽어 응답을 만들게 해, 동시 저장 뒤에도 응답이 실제 저장 상태를 따르게 했다.

검증 결과:

- `./gradlew test --tests com.bubli.user.service.UserServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 사용자 설정 생성 충돌 방지와 잠금 저장

처리 시각: 2026-07-05 KST

변경 내용:

- `user_preferences.user_id`는 유니크이므로 첫 설정 저장 요청이 동시에 들어오면 둘 다 row가 없다고 보고 생성을 시도할 수 있다.
- 설정 저장 전 `INSERT ... ON CONFLICT DO NOTHING`으로 기본 설정 row를 먼저 보장하게 했다.
- 기본 row 보장 후 `PESSIMISTIC_WRITE` 잠금 조회로 읽고, 같은 트랜잭션에서 요청 값을 반영하게 했다.
- 유니크 충돌을 JPA 예외로 잡아 이어가는 대신 PostgreSQL upsert 경로를 써서 트랜잭션 rollback-only 위험을 줄였다.

검증 결과:

- `./gradlew test --tests com.bubli.user.service.UserServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 저장 용량 사용량 중복 row 방지와 동시 증감 안정화

처리 시각: 2026-07-05 KST

변경 내용:

- `storage_usage`의 기존 `(user_id, room_id, storage_scope)` 유니크 제약은 `NULL` 값 때문에 개인/룸 사용량 중복 row를 막지 못할 수 있다.
- 개인 사용량은 `user_id`, 룸 사용량은 `room_id` 기준 부분 유니크 인덱스를 추가해 실제 DB에서 중복 row가 생기지 않게 했다.
- 마이그레이션 시 기존 중복 row는 가장 오래된 row 하나로 합치고, `used_bytes`는 합산, `limit_bytes`는 최댓값으로 정리한다.
- 사용량 증감은 쓰기용 잠금 조회를 거친 뒤 처리해 동시 업로드/삭제에서 값이 덮이는 위험을 줄였다.
- 사용량 row 생성 중 유니크 충돌이 나면 이미 생성된 row를 다시 잠금 조회해서 이어서 증감한다.

검증 결과:

- `./gradlew test --tests com.bubli.storage.service.StorageUsageServiceTest --tests com.bubli.schema.EntityFlywayAlignmentTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### WBS 형제 순서 중복 방지와 자동 순번 재시도

처리 시각: 2026-07-05 KST

변경 내용:

- WBS 항목 생성 시 자동 `orderNo`는 기존처럼 같은 부모 안의 마지막 순서 + 1로 계산한다.
- 동시에 같은 부모 아래 WBS 항목이 생성되어 순서 유니크 제약에 걸리면, 순서를 다시 읽어 최대 3회 재시도한다.
- 명시 `orderNo`가 이미 같은 부모 아래 존재하면 DB 예외가 아니라 400 계열 비즈니스 예외로 거절한다.
- PostgreSQL에서 `parent_id IS NULL`인 루트 WBS 항목은 기존 `(room_id, parent_id, order_no)` 유니크 제약만으로 중복을 막지 못하므로, 루트 항목 전용 부분 유니크 인덱스를 추가한다.
- 마이그레이션 시 기존 루트 WBS 항목의 순서는 `order_no`, `created_at`, `id` 순서로 한 번 정규화한 뒤 인덱스를 만든다.

검증 결과:

- `./gradlew test --tests com.bubli.work.wbs.service.WbsItemServiceTest --tests com.bubli.work.wbs.service.WbsItemPublicServiceImplTest --tests com.bubli.schema.EntityFlywayAlignmentTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 채팅 메시지 room_sequence 충돌 재시도

처리 시각: 2026-07-05 KST

변경 내용:

- 사용자 메시지와 에이전트 응답 메시지 저장 시 방별 `room_sequence`를 `마지막 room_sequence + 1`로 계산하는 기존 흐름은 유지한다.
- 동시에 같은 채팅방 메시지가 저장되어 `uk_chat_messages_room_sequence` 유니크 제약에 걸리면, sequence를 다시 읽어 최대 3회 재시도한다.
- 사용자 메시지는 동시 요청으로 `uk_chat_messages_room_client_message` 유니크 제약에 걸리면 기존 메시지를 다시 읽어 반환하고, 웹소켓 중복 발행을 막는다.
- 메시지는 `saveAndFlush`로 저장해 유니크 충돌을 커밋 시점이 아니라 저장 시점에 확인한다.

검증 결과:

- `./gradlew test --tests com.bubli.chat.service.ChatServiceTest --tests com.bubli.chat.service.ChatMessagePublicServiceImplTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 프로젝트룸 이벤트 sequence 충돌 재시도

처리 시각: 2026-07-05 KST

변경 내용:

- 프로젝트룸 이벤트 저장 시 방별 `sequence`를 `마지막 sequence + 1`로 계산하는 기존 흐름은 유지한다.
- 동시에 같은 방 이벤트가 저장되어 `uk_project_room_events_room_sequence` 유니크 제약에 걸리면, sequence를 다시 읽어 최대 3회 재시도한다.
- 성공한 이벤트만 웹소켓으로 발행해, 실패한 첫 시도가 중복 알림으로 나가지 않게 했다.

검증 결과:

- `./gradlew test --tests com.bubli.project.service.ProjectRoomEventRecorderTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 프로젝트룸 이벤트 actor 조회 배치화

처리 시각: 2026-07-05 KST

변경 내용:

- 프로젝트룸 이벤트 목록 조회 시 이벤트별로 actor 사용자 정보를 개별 조회하지 않는다.
- 응답 페이지 안의 actor userId를 모아 `UserPublicService.getUsers`로 한 번에 조회한다.
- 시스템 이벤트는 사용자 조회 대상에서 제외하고, 탈퇴/삭제 등으로 조회되지 않는 actor는 기존처럼 `Unknown`으로 표시한다.
- 최대 100개 이벤트 조회에서 사용자 조회가 이벤트 개수만큼 늘어나는 N+1 비용을 줄였다.

검증 결과:

- `./gradlew test --tests com.bubli.project.service.ProjectRoomEventServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 에이전트 응답 채팅 멤버 재활성화 안정화

처리 시각: 2026-07-05 KST

변경 내용:

- 프로젝트룸 에이전트 응답 메시지를 채팅에 남길 때, 채팅 멤버 row를 ACTIVE 여부만으로 판단하지 않는다.
- 기존 `chat_room_members` row를 먼저 조회하고, 없으면 생성하며, LEFT 상태면 새 행을 만들지 않고 재활성화한다.
- 에이전트 응답 생성 중 `uk_chat_room_members_room_user` 유니크 제약에 걸릴 수 있는 중복 저장 위험을 줄였다.

검증 결과:

- `./gradlew test --tests com.bubli.chat.service.ChatMessagePublicServiceImplTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 그룹 채팅 멤버 재초대 안정화

처리 시각: 2026-07-05 KST

변경 내용:

- 그룹 채팅 멤버 초대 시 대상 사용자 존재 검증은 기존처럼 유지한다.
- 기존 채팅 멤버를 한 번에 조회하고, 이미 ACTIVE인 멤버는 그대로 둔다.
- LEFT 상태로 남아 있는 멤버는 새 행을 만들지 않고 재활성화한다.
- `chat_room_members`의 방+사용자 유니크 제약에 걸릴 수 있는 재초대 중복 저장 위험을 줄였다.

검증 결과:

- `./gradlew test --tests com.bubli.chat.service.ChatServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 프로젝트룸 채팅 멤버 동기화 안정화

처리 시각: 2026-07-05 KST

변경 내용:

- 프로젝트룸 채팅방을 만들거나 기존 방을 다시 동기화할 때, 활성 프로젝트룸 멤버별 `exists` 조회를 반복하지 않고 기존 채팅 멤버를 한 번에 조회한다.
- 이미 ACTIVE인 채팅 멤버는 그대로 두고, LEFT 상태로 남아 있는 채팅 멤버는 새 행을 만들지 않고 재활성화한다.
- `chat_room_members`의 방+사용자 유니크 제약에 걸릴 수 있는 중복 저장 위험을 줄였다.

검증 결과:

- `./gradlew test --tests com.bubli.chat.service.ChatServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 공통 사용자 배치 조회 최적화

처리 시각: 2026-07-05 KST

변경 내용:

- `UserPublicService.getUsers`에 `Collection<UUID>` 기반 배치 조회 경로를 추가했다.
- 기존 `Page<UUID>` 기반 호출은 유지하되 내부적으로 같은 배치 조회 경로를 쓰게 했다.
- 사용자 ID 목록에서 null과 중복을 제거하고, 유효한 ID가 없으면 DB 조회 없이 빈 결과를 반환한다.
- 보이스챗 참가자 이름 조회는 임시 `PageImpl` 생성 없이 새 배치 조회 경로를 사용한다.
- 탈퇴한 사용자를 배치 조회 응답에서 제외하는 기존 동작은 유지했다.

검증 결과:

- `./gradlew test --tests com.bubli.user.service.UserPublicServiceImplTest --tests com.bubli.voice.service.VoiceRoomServiceTest --tests com.bubli.chat.service.ChatServiceTest --tests com.bubli.project.service.ProjectRoomMemberServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 보이스챗 조회 권한과 참가자 이름 조회 최적화

처리 시각: 2026-07-05 KST

변경 내용:

- 프로젝트룸에 연결된 보이스챗 방 조회, 마이크 상태 변경, 나가기 흐름에서 프로젝트룸 멤버 권한을 다시 확인한다.
- 보이스챗 참가자 이름을 참가자별 개별 조회하지 않고 한 번의 사용자 배치 조회로 가져오도록 바꿨다.
- 참가자 이름 조회 실패 시 기존처럼 빈 이름으로 응답해 보이스챗 응답 전체가 실패하지 않게 유지했다.

검증 결과:

- `./gradlew test --tests com.bubli.voice.service.VoiceRoomServiceTest` 통과
- `./gradlew test --tests '*ArchitectureTest'` 통과
- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과

남은 작업:

- GitHub Actions CI 확인 후 develop 머지 상태를 확인한다.

### 채팅 타이핑 WebSocket 릴레이

처리 시각: 2026-07-04 KST

변경 내용:

- 프론트가 `/app/chat/{chatRoomId}/typing`으로 보낸 타이핑 상태를 서버가 검증 후 `/topic/chat/{chatRoomId}/typing`으로 재발행하도록 추가했다.
- 타이핑 이벤트는 저장하지 않는다. 활성 채팅방 멤버인지 확인하고 `{ chatRoomId, typing, userId, userName }`만 내려준다.
- `/topic/chat/{chatRoomId}/typing` 구독도 기존 채팅방 멤버 권한 검사에 포함했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew test --tests 'com.bubli.chat.service.ChatTypingServiceTest' --tests 'com.bubli.websocket.service.WebSocketSubscriptionAuthorizationServiceTest'` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- 프론트에서 `NEXT_PUBLIC_CHAT_TYPING_RELAY=1`을 켜고 실제 STOMP 송수신을 확인한다.

### 받은 프로젝트룸 초대 직접 조회/수락 흐름 보강

처리 시각: 2026-07-04 KST

변경 내용:

- 초대받은 사용자가 자기 앞으로 온 프로젝트룸 초대 ID를 바로 확인할 수 있도록 `GET /api/me/invitations`를 추가했다.
- 기본 조회 상태는 `PENDING`이며, 응답에는 `roomName`, 초대한 사람의 Bubli ID, 이름, 프로필 이미지도 함께 내려준다.
- 기존 `PATCH /api/invitations/{id}/accept` 흐름은 그대로 유지한다. 새 받은 초대 목록에서 받은 `id`를 그대로 수락 API에 넘기면 멤버가 된다.
- `docs/10_API-Design.md`, `docs/http/user.http`에 받은 초대 조회 API를 반영했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew test --tests com.bubli.project.service.ProjectRoomMemberServiceTest --tests com.bubli.project.controller.ProjectRoomControllerIntegrationTest` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- 프론트 초대함/알림 화면에서 `GET /api/me/invitations?status=PENDING`을 호출하고, 받은 `id`로 수락 버튼을 연결한다.

### Google Calendar WBS 삭제 재동기화 트러블슈팅

처리 시각: 2026-07-03 KST

변경 내용:

- Bubli DB에서는 삭제됐지만 Google Calendar 삭제가 실패한 일정을 `google_calendar_delete_requests`에 남긴다.
- Google Calendar 삭제 API가 404/410을 반환하면 이미 없는 이벤트로 보고 삭제 성공 처리한다.
- `POST /api/calendar/sync`에서 삭제 대기 중인 `googleEventId`가 다시 내려오면 로컬 일정으로 upsert하지 않고 Google 삭제만 재시도한다.
- Google Calendar에서 `cancelled` 상태가 확인된 이벤트는 로컬 일정 삭제와 함께 삭제 대기 상태도 정리한다.
- 트러블슈팅 문서 `docs/troubleshooting/google-calendar-wbs-delete-resync-2026-07-03.md`를 추가했다.

검증 결과:

- `./gradlew test --tests 'com.bubli.personal.calendar.service.GoogleCalendarEventServiceTest' --no-daemon` 통과
- `./gradlew test --no-daemon` 통과
- `git diff --check` 통과

남은 작업:

- 실제 Google 계정으로 삭제 실패/재동기화 상황을 수동 재현해 Google Calendar에서도 최종 삭제되는지 확인한다.

### Google Calendar 외부 삭제 일정 동기화 보강

처리 시각: 2026-07-03 KST

변경 내용:

- `POST /api/calendar/sync`가 Google Calendar 이벤트 목록을 가져올 때 `showDeleted=true`를 함께 보내도록 수정했다.
- Google Calendar 응답의 `status=cancelled` 이벤트를 읽을 수 있도록 `GoogleCalendarEventPayload`에 `status`와 `isCancelled()`를 추가했다.
- Google Calendar에서 사용자가 직접 삭제한 일정은 같은 `googleEventId`를 가진 로컬 `schedules` row를 삭제한다.
- 단순히 Google 목록에 없다는 이유로 로컬 일정을 지우지는 않는다. 기간 밖으로 이동한 일정까지 지워지는 위험을 막기 위해 `cancelled` 상태가 명시된 이벤트만 삭제 대상으로 본다.
- WBS 기간 삭제 API(`DELETE /api/schedules/{scheduleId}`)와 별개로, 외부 Google Calendar 삭제가 다음 sync 때 Bubli DB에 반영되는 흐름을 보강했다.

검증 결과:

- `./gradlew test --tests 'com.bubli.personal.calendar.service.GoogleCalendarEventServiceTest'` 통과

남은 작업:

- 실제 Google 계정에서 WBS 기간 일정을 만든 뒤 Google Calendar에서 직접 삭제하고, `POST /api/calendar/sync` 후 Bubli 화면에서 사라지는지 수동 확인한다.

### Google OAuth, JWT, LiveKit 운영 설정 연결

처리 시각: 2026-07-02 09:39 KST

변경 내용:

- 받은 Google OAuth client, JWT secret, LiveKit key/secret/server URL은 로컬 `src/main/resources/application-secret.yml`에만 적용했다.
- `application-secret.yml`은 `.gitignore` 대상임을 확인했고 커밋하지 않는다.
- `src/main/resources/application-secret.yml.example`을 추가해 팀원이 같은 키 구조로 로컬 secret 파일을 만들 수 있게 했다.
- 운영 `docker-compose.prod.yml`에 Google OAuth, Google Calendar, LiveKit server URL 환경변수를 추가했다.
- `docs/SECRETS.md`에 GitHub Actions/EC2 배포에 필요한 Google OAuth, Google Calendar, LiveKit server URL secret 항목을 추가했다.
- `/oauth2/authorization/google` 경로로 접근해도 프론트 404로 떨어지지 않도록 백엔드 redirect endpoint와 nginx `/oauth2/` 프록시를 추가했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew test --tests '*AuthServiceTest' --tests '*VoiceRoomServiceTest'` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

남은 작업:

- GitHub Secrets와 서버 `.env`에 실제 값을 등록한 뒤 배포해야 운영 로그인과 LiveKit 연결이 실제로 동작한다.
- Google Cloud Console의 승인된 redirect URI에 `https://bubli.n-e.kr/auth/callback`, `https://bubli.n-e.kr/calendar/google/callback`, 로컬 개발용 URI를 등록해야 한다.

### 작업 카드 6-5. storage usage 조회 API

처리 시각: 2026-06-25 03:48 KST

변경 내용:

- `feature/storage-usage-api`를 #31 `feature/resource-related-api` 위의 새 브랜치로 만들었다.
- 현재 `10_API-Design.md` 기준 `GET /api/storage/usage`를 추가했다.
- 현재 사용자 개인 `storage_usage`와 ACTIVE로 참여 중인 프로젝트룸의 ROOM scope `storage_usage`를 함께 조회한다.
- `StorageUsageRepository`, `StorageUsageService`, `StorageController`를 추가했다.
- 응답은 Entity를 직접 반환하지 않고 `StorageUsageResponse`와 `StorageUsageResult` 계열 DTO로 분리했다.
- 각 usage row의 `remainingBytes`와 전체 `totalUsedBytes`, `totalLimitBytes`, `totalRemainingBytes`를 계산해 반환한다.
- `docs/http/resource.http`에 수동 검증 예시를 추가했다.
### 작업 카드 8. Entity/Flyway 정합성 점검

처리 시각: 2026-06-24 21:42:53 KST

변경 내용:

- `EntityFlywayAlignmentTest`를 추가했다.
- 엔티티의 `@Table/@Column` 선언과 `V1__init_schema.sql`의 `CREATE TABLE` 컬럼을 대조한다.
- Docker/Testcontainers가 꺼져 통합 테스트가 스킵되는 환경에서도 기본적인 테이블/컬럼 누락을 잡을 수 있게 했다.
- `@EmbeddedId` 기반 복합키는 별도 `id` 컬럼으로 오해하지 않도록 제외했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #39는 #31 위의 stacked PR이라 현재 base 체인에서는 workflow가 실행되지 않았다. base 체인 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 저장 용량 조회만 다룬다.
- 파일 업로드, resource download-url, LocalFileStorage/S3Storage 구현은 별도 PR로 남긴다.
- 이번 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 6-4. resource_relations 관련 자료 조회 API

처리 시각: 2026-06-25 02:40 KST

변경 내용:

- `feature/resource-related-api`를 #25 `feature/resource-basic-foundation` 위의 새 브랜치로 만들었다.
- `GET /api/resources/{resourceId}/related`를 추가했다.
- `resource_relations`를 기준으로 관련 자료 목록을 조회한다.
- 기준 자료와 관련 자료 모두 사용자 접근 권한을 확인한다.
- API 응답은 Entity를 직접 반환하지 않고 `ResourceRelatedResponse`와 `ResourceRelatedResult`로 분리했다.
- 관련 자료 관계 저장/생성 API는 만들지 않았다. 에이전트 분석 저장 흐름이 정리되면 별도 PR에서 다룬다.
- `docs/http/resource.http`에 관련 자료 목록 수동 검증 예시를 추가했다.
- GitHub Actions checks 없음. #22, #23, #24, #25, #26 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 현재 기준 테스트에서는 엔티티 테이블/컬럼 누락이 발견되지 않았다.
- 이 테스트는 타입, FK, 인덱스, enum 값까지 검증하지 않는다.
- Docker가 켜진 환경의 Testcontainers/Flyway validate 보강은 다음 카드에서 다룬다.

### 작업 카드 7-2. agent job 상태 조회 API

처리 시각: 2026-06-25 02:44 KST

변경 내용:

- `feature/agent-job-status-api`를 #26 `feature/agent-storage-foundation` 위의 새 브랜치로 만들었다.
- 현재 `10_API-Design.md` 기준 `GET /api/agent-jobs/{jobId}`를 추가했다.
- 로그인 사용자 본인이 요청한 `agent_jobs`만 조회한다.
- `AgentJobResponse` DTO를 추가해 Entity를 직접 반환하지 않게 했다.
- 존재하지 않거나 본인 job이 아니면 `AGENT_404_001`로 응답하도록 Service를 보정했다.
- `docs/http/agent.http`에 수동 검증 예시를 추가했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks는 PR 생성 후 확인한다.

메모:

- 현재 6/25 `10_API-Design.md`의 `GET /api/resources/{id}/related`를 작업 기준선으로 삼았다.
- S3 download-url, ai-document 상세 API는 섞지 않고 후속 PR로 남긴다.
- 이번 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

- 이번 PR은 agent job 상태 조회만 다룬다.
- `POST /api/ai/*` 작업 생성 API는 권한, 분석 제한, 큐 연결 정책이 더 필요해서 후속 PR로 남긴다.
- agent가 tasks, wbs_items, schedules, memos를 직접 확정 저장하는 흐름은 넣지 않았다.
- 이번 변경에는 Gradle, GitHub Actions, PR 템플릿, README, SETUP 같은 초기 개발환경 세팅 파일 변경이 없다.

### 작업 카드 7-1. 6/25 기준 agent enum 보정

처리 시각: 2026-06-25 00:43 KST

변경 내용:

- 6/25 백엔드 개발 가이드와 `10_API-Design.md` 기준으로 `AgentSuggestionType`을 확장했다.
- `TODO`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`, `DOCUMENT_DRAFT`, `DAILY_SUMMARY`, `MEMO` 후보 타입을 추가했다.
- 기존 데이터 모델 표에 남아 있는 `TASK`, `REVIEW_ITEM` 값은 바로 제거하지 않고 호환값으로 유지했다.
- `AgentJobType`에 요구사항 생성, 계약 문서 검토, 질문 생성, 하루정리, 자료 검색, 문서 초안 작업 타입을 추가했다.
- `AgentStorageServiceTest`의 후보 저장 검증을 6/25 기준 `TODO` 후보로 바꿨다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #26은 workflow 보강 전 생성된 stacked PR이라 #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 변경은 agent 저장 enum 보정만 다룬다.
- agent Controller/API 연결은 별도 PR로 분리한다.
- 후보 승인 후 확정 데이터 생성은 여전히 target 도메인 Service가 맡는다.

### 작업 카드 7. agent 저장 기반

처리 시각: 2026-06-24 21:39:10 KST

변경 내용:

- `AgentJob`, `AgentSuggestion`, `AiDocument` 저장 기반을 추가했다.
- `AgentJobRepository`, `AgentSuggestionRepository`, `AiDocumentRepository`를 추가했다.
- `AgentJobService`, `AgentSuggestionService`, `AiDocumentService`를 추가했다.
- Service 입력/반환은 Command/Result 객체로 분리했다.
- `AgentSuggestion`은 후보를 `DRAFT`로 저장하며, TODO/WBS/일정/메모 같은 확정 데이터는 만들지 않는다.
- agent 서비스에서 다른 도메인의 Repository나 Entity를 직접 호출하지 않았다.
- `AgentStorageServiceTest`로 job 생성, 실패 상태 전이, 후보 DRAFT 저장, AI 문서 READY 저장을 검증했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 내부 저장 기반이다. 에이전트 Controller/API 연결은 별도 PR로 분리한다.
- 후보 승인 후 확정 데이터 생성은 target 도메인 Service에서 처리해야 한다.
- `AgentSuggestionType` enum은 현재 코드 기준 후보 타입만 있으며, 최종 API/기획 기준 타입 확장이 필요할 수 있다.
- Flyway의 `agent_model_call_logs` 테이블 정의와 `AgentModelCallLog` 엔티티가 맞지 않는 흔적이 있어 Entity/Flyway 정합성 카드에서 확인해야 한다.

### 작업 카드 6. resource 기본 저장/조회 기반

처리 시각: 2026-06-24 21:34:16 KST

변경 내용:

- 현재 `10_API-Design.md`를 기준으로 자료 기본 저장/조회 API 뼈대를 추가했다.
- `ResourceController`, `ResourceService`, `ResourceRepository`를 추가했다.
- `GET /api/resources?scope=personal`, `GET /api/project-rooms/{roomId}/resources`, `POST /api/resources`, `GET /api/resources/{resourceId}`를 구현했다.
- API 응답은 Entity를 직접 반환하지 않고 `ResourceResponse`와 `ResourceResult`로 분리했다.
- 개인 자료는 `owner_id`, 프로젝트룸 자료는 `room_id`와 ACTIVE `room_members` 기준으로 접근을 확인한다.
- `docs/http/resource.http`에 현재 API 기준 수동 검증 예시를 추가했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- 이번 PR은 자료 카드 메타데이터 저장/조회 기반이다.
- 실제 파일 업로드, S3 저장, 다운로드 URL, 버전, 댓글, 요약/AI 문서 API는 별도 PR로 분리한다.
- API 예시의 자료 상태값 `UPLOADED`, `ARCHIVED`와 현재 DB/코드 enum `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED` 사이 차이는 최종 API 수정본에서 보정이 필요하다.

### 작업 카드 3-1. 6/25 기준 Google auth endpoint 보정

처리 시각: 2026-06-25 00:47 KST

변경 내용:

- 6/25 `10_API-Design.md` 기준에 맞춰 `POST /api/auth/login` 뼈대를 제거했다.
- `GET /api/auth/google/authorize` endpoint 뼈대를 추가했다.
- `POST /api/auth/google/callback` endpoint 뼈대를 추가했다.
- 보안 허용 경로를 Google authorize/callback, refresh 기준으로 바꿨다.
- `.http` 예시를 Google OAuth code callback 기준으로 수정했다.
- 실제 Google OAuth 검증, authorize URL 생성, refresh token rotation은 기존처럼 501 TODO로 남겼다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과
- GitHub Actions checks 없음. #24는 workflow 보강 전 생성된 stacked PR이라 #22, #23 merge 후 `develop` 기준 CI 재확인 필요.

메모:

- signup, email/password 로그인은 되살리지 않았다.
- 이번 변경은 auth endpoint surface와 예시만 6/25 기준으로 맞춘다.
- 기초 설정, Gradle, GitHub Actions, PR 템플릿, README는 건드리지 않았다.

### 작업 카드 2. 프로젝트룸 권한 검사 서비스 분리

처리 시각: 2026-06-24 21:01:54 KST

변경 내용:

- `project.service.RoomAccessService`를 추가해 프로젝트룸 접근 권한 확인을 공통 Service로 분리했다.
- `room_members.status=ACTIVE` 확인용 `isActiveMember`, `validateActiveMember` 메서드를 제공했다.
- `PROJECT_LEADER` 권한 확인용 `isProjectLeader`, `validateProjectLeader` 메서드를 제공했다.
- `RoomMemberRepository`에 ACTIVE + role 기준 exists 메서드를 추가했다.
- `ProjectRoomService`, `ScheduleService`의 직접 ACTIVE 멤버 확인 로직을 `RoomAccessService` 호출로 바꿨다.
- `RoomAccessServiceTest`를 추가하고, 기존 `ProjectRoomServiceTest`, `ScheduleServiceTest`를 새 권한 Service 기준으로 수정했다.

검증 결과:

- `./gradlew compileTestJava` 통과
- `./gradlew cleanTest test` 통과
- `git diff --check` 통과

메모:

- 새 Controller endpoint, 초대/멤버/일정 API 요청/응답 모양은 건드리지 않았다.
- 일정 목록 조회의 JPA Specification 안에 있는 `room_members.status=ACTIVE` subquery는 조회 필터라서 유지했다.
- 이 작업은 #22 `feature/schedule-basic-api` 위에서 이어지는 foundation 작업이다.
- stacked PR로 생성하면 현재 CI 설정상 GitHub Actions가 실행되지 않을 수 있다.
- #22 merge 후 `develop` 기준으로 GitHub Actions CI를 다시 확인해야 한다.

## 열린 PR 상태

| PR | 제목 | 브랜치 | 확인한 head | 현재 메모 |
|---|---|---|---|---|
| #19 | `feat: 프로젝트룸 멤버 초대 API 추가` | `feature/project-room-members-invitations` | `4a16d9e` | 최신 API Design의 이메일/사용자 ID 초대와 초대 링크 API 기준 재검토 필요 |
| #20 | `feat: 채팅 기본 API 추가` | `feature/chat-basic-api` | `f13a43e` | 기본 채팅 API는 진행됨. `POST /api/chat/direct-rooms` 누락 여부 확인 필요 |
| #21 | `feat: 작업 WBS 기본 API 추가` | `feature/work-task-wbs-api` | `4dd06c6` | 작업/WBS 기본 API는 진행됨. 대시보드 TODO, WBS board, reorder, time-log 분리 여부 확인 필요 |
| #22 | `feat: 일정 기본 API 추가` | `feature/schedule-basic-api` | `3e2a7bf` | 일정 API는 기준과 대체로 맞음. Google Calendar 연동 범위 표기가 기획과 맞는지 확인 필요 |
| #23 | `feat: 프로젝트룸 권한 검사 서비스 분리` | `feature/room-access-service` | `5aa677a` | #22 기준 draft stacked PR 생성됨. GitHub Actions checks 없음. #22 merge 후 `develop` 기준 CI 재확인 필요 |
| #24 | `chore: Google-only 인증 기반 정리` | `feature/auth-google-foundation` | `15f9b7d` | #23 기준 draft stacked PR. 6/25 Google authorize/callback endpoint 보정 완료. GitHub Actions checks 없음. #22, #23 merge 후 `develop` 기준 CI 재확인 필요 |
| #25 | `feat: 자료 기본 저장 조회 API 추가` | `feature/resource-basic-foundation` | `14c522d` | #24 기준 draft stacked PR. 6/25 자료 수정/삭제 보정과 #24 base 병합 완료. GitHub Actions checks 없음. #22, #23, #24 merge 후 `develop` 기준 CI 재확인 필요 |
| #26 | `feat: 에이전트 저장 기반 추가` | `feature/agent-storage-foundation` | `local latest` | #25 기준 draft stacked PR. 6/25 agent enum 보정 완료. GitHub Actions checks 없음. #22, #23, #24, #25 merge 후 `develop` 기준 CI 재확인 필요 |
| #27 | `test: Entity Flyway 정합성 검사 추가` | `feature/entity-flyway-alignment` | `a7e7692` | #26 기준 stacked PR. 최신 #26 병합 후 로컬 검증과 PR 상태 재확인 필요 |
| #32 | `[feat] 에이전트 작업 상태 조회 API 추가` | `feature/agent-job-status-api` | latest pushed | #26 기준 draft stacked PR 생성 완료. 로컬 검증 통과. GitHub Actions checks 없음. base #26에는 #28의 stacked PR CI workflow가 아직 포함되지 않음 |

## API Design 기준 재검토 후보

| 영역 | API Design 기준 | 현재 판단 |
|---|---|---|
| 프로젝트룸 초대 | 최신 기획 기준은 수락된 친구/가입 사용자 대상 초대만 유지하고 링크 초대는 제외 | 링크 초대 HTTP 컨트롤러와 공개 허용 설정은 제거됨. 남은 DB/내부 타입은 별도 마이그레이션 영향 검토 후 정리 필요 |
| 인증 | 6/25 기준 `GET /api/auth/google/authorize`, `POST /api/auth/google/callback`, refresh, logout | #24에서 signup/email-password는 되살리지 않고, `POST /api/auth/login`을 Google authorize/callback endpoint 뼈대로 보정한다 |
| 자료 상태값 | `ResourceResponse.status` 예시는 `UPLOADED`, `ANALYZING`, `ANALYZED`, `FAILED`, `ARCHIVED` | 현재 데이터 딕셔너리와 코드 enum은 `UPLOADING`, `READY`, `ANALYZING`, `ANALYZED`, `FAILED`, `DELETED`이므로 최종 API 수정본에서 상태값 명칭 보정 필요 |
| 자료 업로드 | `POST /api/resources`는 개인 또는 프로젝트룸 자료 업로드 | #25는 파일/S3 업로드 전 단계의 자료 카드 메타데이터 저장/조회 기반만 구현함. multipart 업로드, 파일 메타데이터, 버전 생성은 별도 PR 필요 |
| 저장 용량 | `GET /api/storage/usage`는 사용자별 서버 저장 용량과 남은 용량 조회 | `feature/storage-usage-api`에서 개인/참여 룸 usage row 조회와 합계 계산을 추가한다 |
| 에이전트 후보 타입 | 기획/가이드는 TODO, WBS, REQUIREMENT, SCHEDULE, QUESTION, CONTRACT_FIELD, CONTRACT_REVIEW, DOCUMENT_DRAFT, DAILY_SUMMARY, MEMO 등 후보를 통합 저장한다고 설명 | #26에서 6/25 기준 타입을 추가했다. 기존 `TASK`, `REVIEW_ITEM`은 09_Data-Model 표와 기존 저장값 호환을 위해 유지했다 |
| Entity/Flyway | `agent_model_call_logs` 엔티티와 Flyway 테이블 정의 | Flyway 정의가 모델 호출 로그가 아니라 agent suggestion 형태 컬럼을 가진 것으로 보인다. 별도 정합성 PR에서 확인 필요 |
| 채팅 | `POST /api/chat/direct-rooms` 포함 | #20에 1:1 채팅방 생성/조회 API가 있는지 확인 필요 |
| 작업 대시보드 | `GET /api/dashboard/tasks` 포함 | #21 또는 별도 dashboard 작업으로 분리할지 결정 필요 |
| WBS 작업판 | `GET /api/project-rooms/{roomId}/wbs-board` 포함 | #21에 WBS board 통합 조회가 있는지 확인 필요 |
| 타이머 | `POST /api/time-logs/start`, pause, resume, stop, heartbeat 포함 | #21에 섞지 말고 `personal/timer` 또는 별도 time-log PR로 분리하는 편이 안전함 |
| 일정 | `GET/POST/PATCH/DELETE /api/schedules` | #22와 대체로 맞음. Google Calendar 직접 연동은 별도 PR로 분리 가능 |

## 구조 검토 메모

- 현재 코드에는 `personal/*`, `work/*` 하위 도메인 패키지가 들어와 있다. 이 방향은 팀이 말한 패키지 구조와 맞다.
- `global`은 공통 설정, 응답, 에러, 보안, 검증 코드만 두는 기준으로 유지한다.
- `global/entity`나 공통 `BaseTimeEntity`는 만들지 않는다.
- `createdAt`, `updatedAt`은 각 엔티티 필드로 직접 둔다.
- `local_*` Tauri SQLite 테이블은 서버 JPA 엔티티로 만들지 않는다.

## 다음 작업 우선순위

1. #23은 #22 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
2. #24는 #22, #23 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
3. #25는 #22, #23, #24 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
4. #26은 #22, #23, #24, #25 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
5. #27은 #22~#26 merge 후 `develop` 기준으로 GitHub Actions CI를 재확인한다.
6. `docs/CURRENT_API_BASELINE_WORK.md` 기준으로 다음 작업 1개를 선택한다.
7. 추천 다음 작업은 Testcontainers/CI 기반 보강이다. Docker가 켜진 환경에서 Flyway validate와 JPA schema validate가 확실히 돌도록 확인한다.
8. #19~#27은 현재 API 기준으로 계속 진행하되, 수정본 API가 오면 차이 보정한다.

## 현재 API 기준 가능한 작업

| 우선순위 | 작업 | 메모 |
|---|---|---|
| 1 | 패키지 구조와 도메인 위치 점검 | `personal/*`, `work/*`, `project`, `resource`, `agent` 등 기존 구조 기준 |
| 2 | Entity, Enum, Repository, Flyway 기준 점검 | API보다 데이터 딕셔너리를 우선한다 |
| 3 | Security/JWT/CurrentUser 기반 점검 | 모든 API에서 재사용되는 공통 기반 |
| 4 | 공통 응답, 공통 에러, Validation 점검 | API 확정 뒤 Controller에 붙일 기반 |
| 5 | 프로젝트룸 권한 검사 서비스 점검 | `room_members.status=ACTIVE`, `PROJECT_LEADER` 권한 확인 |
| 6 | Testcontainers와 테스트 support 점검 | API 확정 뒤 PR별 테스트를 빠르게 붙이기 위함 |
| 7 | `.http` 파일 구조 정리 | 현재 API 기준 요청 예시를 맞춘다 |

상세 작업 지시와 복붙 프롬프트는 `docs/CURRENT_API_BASELINE_WORK.md`를 따른다.

## 작업 시 주의할 점

| 주의할 점 | 이유 |
|---|---|
| 여러 기능을 한 브랜치에 섞지 않기 | API 수정본이 오면 보정하기 어렵다 |
| 현재 API를 최종 확정이라고 쓰지 않기 | 지금은 작업 기준선이다 |
| 기획/DB와 충돌하는 API는 기록하기 | 현재 API만 보고 무리하게 확정하지 않는다 |
| Agent/RAG payload 과확정 금지 | 에이전트 흐름은 변경 가능성이 높다 |
| WebSocket payload 과확정 금지 | 채팅/이벤트 범위 변경 가능성이 있다 |

## API 명세 완성본 수신 절차

사용자가 새 API 명세서를 주면서 완성본이라고 말하면 아래를 수행한다.

1. 새 API 명세 파일 위치를 확인한다.
2. 기존 `/Users/maren/EDU/Final Project/00_현재_프로젝트/최종_산출물/01_기획최종본_2026-06-22/10_API-Design.md`와 비교한다.
3. 새 파일이 기준이면 기준 문서 지도와 스킬의 API 기준 경로를 갱신한다.
4. 이 문서의 `현재 작업 모드`를 새 API 기준 작업 모드로 바꾼다.
5. `API Design 기준 재검토 후보`를 새 API 기준으로 다시 작성한다.
6. 현재 구현과 새 API의 endpoint, DTO, 테스트 차이를 다시 정리한다.
7. 이후 한 PR씩 차이 보정 작업을 하고 로컬 검증과 GitHub Actions CI를 확인한다.

## 작업 완료 기준

문서만 수정한 경우:

- `ls docs/00_BACKEND_START_HERE.md docs/WORK_HANDOFF.md docs/CODEX_BACKEND_WORKFLOW.md`
- `rg "10_API-Design.md|최종기획|데이터딕셔너리|WORK_HANDOFF|GitHub Actions|CI" docs`
- 예전 다운로드 폴더의 API Design 절대경로가 문서나 코드에 남아 있지 않은지 확인한다.
- `git diff --check`

코드를 수정한 경우:

- `./gradlew compileTestJava`
- `./gradlew cleanTest test`
- `git diff --check`
- GitHub Actions CI 통과 확인

## 갱신 규칙

- PR을 새로 만들거나 수정하면 이 문서의 열린 PR 상태를 갱신한다.
- 기준 문서와 맞지 않는 부분을 발견하면 `API Design 기준 재검토 후보`에 추가한다.
- API 명세 완성본이 들어오면 `현재 작업 모드`, API 기준 경로, PR 재검토 후보를 갱신한다.
- 작업 완료 후 마지막 확인 시각을 갱신한다.
- CI가 아직 확인되지 않았으면 완료로 쓰지 않는다.
