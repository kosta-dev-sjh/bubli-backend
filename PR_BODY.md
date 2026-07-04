# 프로젝트룸별 구글 캘린더 생성·매핑

## 작업 내용

WBS/룸 일정이 사용자의 구글 캘린더 primary가 아니라 프로젝트룸 이름으로 만든 별도 캘린더에 기록되도록 한다. 캘린더는 룸당·사용자당 1개를 지연 생성한다.

## 변경 사항

- `GoogleCalendarClient.insertCalendar(summary)` 추가 — `POST /calendar/v3/calendars` (Asia/Seoul)
- OAuth scope `calendar.events` → `calendar` 확장 (calendars.insert에 필요)
- `project_room_google_calendars` 테이블 (V19 마이그레이션, user_id+room_id 유니크)
- `ProjectRoomCalendarService.ensureRoomCalendar` — 연결 시 지연 생성, 동시성은 유니크 제약으로 처리
- `GoogleCalendarScheduleSyncService` — roomId 있는 일정은 룸 캘린더로 라우팅, 개인 일정은 primary 유지, 기존 동기화 이벤트는 이동하지 않음
- `markSynced`가 실제 calendarId/summary 기록 → 수정/삭제가 올바른 캘린더 대상
- groups 응답의 PROJECT_ROOM 그룹에 매핑된 googleCalendarId 포함
- 신규: `GET /api/calendar/rooms/{roomId}/calendar` → `{ googleCalendarId, calendarName, connected }`

## 테스트 방법

1. 구글 캘린더 연결(재동의 필요 — scope 확장) 후 프로젝트룸 WBS에서 기간 있는 작업 생성
2. 구글 캘린더에 룸 이름 캘린더가 생성되고 일정이 그 캘린더에 기록되는지 확인
3. `GET /api/calendar/rooms/{roomId}/calendar` 응답 확인
4. `./gradlew compileJava` (완료), 통합 테스트는 구글 자격 필요

## 체크리스트

- [x] compileJava 통과 (JDK 21)
- [x] Flyway V19 마이그레이션 추가
- [ ] 기존 연결 사용자 재동의 안내 필요 (scope 확장)
- [ ] 스테이징에서 실제 구글 계정으로 E2E 확인
