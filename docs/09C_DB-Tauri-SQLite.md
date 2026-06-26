# DB Tauri SQLite

## Tauri 로컬 DB

Tauri SQLite는 사용자의 기기 안에서 빠른 표시, 오프라인 보조, 비정상 종료 복구를 맡는다. 서버 DB 원본과 구분해야 한다.

### 로컬 저장 원칙

|원칙|내용|
|---|---|
|사용자 선택|사용자가 고른 폴더와 사용자가 켠 개인 자료함 동기화만 다룬다|
|서버 원본 구분|채팅, TODO, 일정, 알림, 자료 원본은 서버 DB를 우선한다|
|로컬 원문 보호|개인 에이전트 원문과 위젯 상세 이벤트는 서버에 올리지 않는다|
|복구 가능 데이터|서버 원본이 있는 캐시는 손상되면 다시 만든다|
|중복 전송 방지|서버 반영 대기열은 idempotency_key를 사용한다|

### 개인 관리 폴더와 파일 색인

|테이블|역할|서버 반영 기준|
|---|---|---|
|managed_folders|사용자가 등록한 개인 관리 폴더 설정|폴더 자체는 로컬 설정이며 동기화가 켜진 폴더의 파일만 서버 개인 자료로 반영|
|local_files|파일명, 경로, 크기, checksum, 상태|동기화가 켜진 항목은 SYNC_PENDING을 거쳐 resources/resource_versions로 반영|
|local_file_events|추가, 수정, 삭제, 이동 이벤트|개인 자료함 동기화가 켜진 경우 백엔드 localsync API로 전송|
|sync_operation_logs|업로드, 수정, 삭제, 충돌 처리 같은 로컬 작업 로그|로컬 작업 추적용|

local_files와 local_file_events는 Tauri 앱의 SQLite에 저장되는 클라이언트 데이터다. 서버 DB에는 사용자가 개인 자료함 동기화를 켠 관리 폴더 변경분이나 직접 업로드한 파일만 resources와 resource_versions로 반영한다. 프로젝트룸 자료로 자동 공유하지 않는다.

### 채팅과 에이전트 로컬 데이터

|테이블|역할|복구 기준|
|---|---|---|
|local_room_message_cache|프로젝트룸 최근 채팅 캐시|서버 chat_messages와 room_sequence로 재생성|
|local_room_cache_state|채팅 캐시 동기화 상태|VALID, STALE, REBUILDING, CORRUPTED|
|local_agent_messages|개인 에이전트 최근 원문 대화|서버 복구 대상 아님|
|local_agent_summaries|개인 에이전트 로컬 요약|서버 복구 대상 아님|

### 위젯 로컬 데이터

|테이블|역할|서버 반영 기준|
|---|---|---|
|local_widget_display_cache|최근 위젯 표시 데이터 캐시|서버 원본으로 재생성 가능|
|local_widget_usage_events|열기, 닫기, 클릭, 확인 같은 상세 이벤트|서버에 원문 저장하지 않음|
|local_widget_usage_rollups|날짜별, 버블별 로컬 집계|POST /api/widget/usage-summaries로 서버 반영|
|local_timer_state|실행 중 타이머 복구 상태|서버 time_logs와 비교|
|local_sync_outbox|서버 반영 대기열|네트워크 복구 후 재전송|

### 백업

|테이블|역할|기준|
|---|---|---|
|local_backup_manifest|백업 파일 목록과 checksum|최근 일일 백업 7개, 주간 백업 3개|

백업은 개인 SQLite 전체를 무작정 서버에 올리는 방식이 아니다. 로컬 기기에 저장하고, 가능하면 압축과 암호화를 적용한다. 서버에 이미 확정된 데이터는 서버에서 다시 내려받고, 개인 에이전트 원문처럼 로컬에만 있던 데이터는 백업이 없으면 복구하지 못한다.
