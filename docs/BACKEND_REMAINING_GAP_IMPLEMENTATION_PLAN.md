# 백엔드 잔여 기능 구현 계획

작성일: 2026-06-30

## 1. 점검 기준

기준 문서:

- `docs/10_API-Design.md`
- `docs/09_Data-Model.md`
- `docs/08_Tech-Architecture.md`
- `docs/AI_FINAL_PLAN_GAP_IMPLEMENTATION.md`
- 최종 기획서 `Bubli_최종기획_완성본_v15_DB회의반영_2026-06-24.md`

현재 코드는 프로젝트룸, 자료, 작업, WBS, 일정, 채팅, 보이스, 알림, 에이전트 작업의 주요 API가 상당 부분 구현되어 있다.
다만 제품 완성 기준으로는 아래 기능이 부족하다.

## 2. 잔여 기능 요약

| 구분 | 부족한 부분 | 현재 상태 | 우선순위 |
|---|---|---|---|
| AI/RAG | DOCX/Markdown/OCR/HWP 등 문서 분석 포맷 확장 | PDF, text/plain 중심 | P1 |
| AI/RAG | 하루정리 context 고도화 | TODO, 일정 중심 | P1 |
| AI/RAG | 문서 초안 확정 저장 흐름 | `DOCUMENT_DRAFT` 후보 보존 중심 | P1 |
| AI/RAG | 채팅 에이전트 `SUGGEST` 후보 생성 연결 | 응답/메모리 저장만 수행 | P2 |
| AI/RAG | 일부 후보 타입 도메인 반영 | `REQUIREMENT`, `QUESTION`, `REVIEW_ITEM`, `CONTRACT_FIELD`, `CONTRACT_REVIEW`, `MEMO`는 보존 처리 | P2 |
| 위젯 | 위젯 설정, context, item state, 사용 집계 API | 엔티티만 있고 Controller/Service 부재 | P1 |
| 로컬 동기화 | Tauri 개인 관리 폴더 변경분 서버 반영 API | package 설명만 있고 구현 부재 | P1 |
| 활동 기록 | 현재 앱/창 제목 기반 activity API | 엔티티만 있고 Controller/Service/Repository 부재 | P1 |
| 메모 | 개인/프로젝트룸 메모 CRUD | 엔티티만 있고 API 부재 | P2 |
| 대시보드 | 대시보드 조합 데이터 확장 | 작업, 일정, 알림, 타이머 중심. 에이전트 제안/메모/자료 제안 부족 | P2 |
| 보이스 | 보이스 이벤트/참여자 실시간 갱신 | 기본 방/토큰/참여 API 중심 | P2 |
| 이벤트 | 프로젝트룸 이벤트 발행 범위 | 일부 도메인 이벤트 보충 필요 | P2 |
| 운영 | 실제 provider token/cost 기록 | 문자열 길이 기반 추정 | P3 |
| 검증 | E2E 시나리오와 http 파일 정리 | 도메인별 일부만 존재 | P3 |

## 3. 구현 계획

### G-1. 위젯 API 구현

목표:

- `GET/PATCH /api/widget/settings`
- `GET/PATCH /api/widget/context`
- `PATCH /api/widget/items/{id}/state`
- `POST /api/widget/usage-summaries`
- `GET /api/widget/usage-summaries/today`
- `GET /api/widget/summary`

작업:

1. `widget` Repository 추가
   - `WidgetContextSettingRepository`
   - `WidgetBubbleSettingRepository`
   - `WidgetItemStateRepository`
   - `WidgetDailySummaryRepository`
2. `WidgetService` 추가
   - selected room 접근 권한 검증
   - bubble별 설정 upsert
   - item state upsert
   - `rollupKey` 기준 사용 집계 중복 방지
3. `WidgetSummaryService` 추가
   - TODO, 일정, 알림, 타이머, 에이전트 제안 요약 조합
   - widget 테이블에 원본 데이터를 복사 저장하지 않음
4. Controller/DTO 추가
5. 통합 테스트 추가

완료 기준:

- 위젯 context가 접근 권한 없는 프로젝트룸으로 설정되지 않는다.
- 같은 `rollupKey` 재전송 시 집계가 중복 반영되지 않는다.
- `GET /api/widget/summary`가 개인 모드와 프로젝트룸 모드에서 각각 동작한다.

### G-2. 로컬 동기화 API 구현

목표:

- `POST /api/local-file-events/sync`

작업:

1. 서버 DB 대상 테이블 범위 확정
   - `local_*` SQLite 테이블은 JPA 엔티티로 만들지 않음
   - 서버에는 sync job, idempotency, 반영 결과만 저장
2. `LocalSyncJob`, `LocalSyncEventRecord` 또는 기존 테이블 대체 가능성 검토
3. idempotency key 검증
4. 개인 자료함 반영은 사용자 승인된 이벤트만 처리
5. 프로젝트룸 자동 공유 금지 검증
6. 실패 이벤트와 재시도 가능 여부 응답

완료 기준:

- 같은 idempotency key 요청은 중복 반영되지 않는다.
- 개인 자료 이벤트가 프로젝트룸 자료로 자동 공유되지 않는다.
- 실패 항목을 클라이언트가 재시도할 수 있게 응답한다.

### G-3. 활동 기록 API 구현

목표:

- `POST /api/activity/current-app`
- `GET /api/activity/today`
- `DELETE /api/activity/{id}`

작업:

1. `ActivityLogRepository` 추가
2. `ActivityService` 추가
   - privacy consent 확인
   - 앱 이름, 창 제목, duration 저장
   - 민감 원문 저장 금지
3. 오늘 활동 조회는 사용자 timezone 기준으로 처리
4. 하루정리 context collector와 연결
5. Controller/DTO/테스트 추가

완료 기준:

- 활동 감지 동의가 꺼져 있으면 기록하지 않는다.
- 화면 전체 내용, 키보드 입력, 파일 원문은 저장하지 않는다.
- 하루정리 생성 context에 activity summary가 포함된다.

### G-4. 하루정리 AI context 고도화

목표:

- 하루정리가 실제 사용자 활동 기반으로 생성되게 한다.

작업:

1. `AgentJobContextCollector`에서 하루정리 context 확장
   - 완료 TODO
   - 남은 TODO
   - 오늘 일정
   - 읽지 않은 알림
   - 타이머/time log
   - 메모
   - widget daily summary
   - activity log summary
2. summary date/timezone 정책 정리
3. `DAILY_SUMMARY` LLM output schema 강화
4. 승인 시 `daily_summaries` upsert 유지
5. 개인정보 원문 저장 금지 테스트 추가

완료 기준:

- `POST /api/ai/summarize-day` 결과가 TODO/일정 외 타이머, 활동, 알림 근거를 포함한다.
- 같은 날짜 승인 시 중복 row를 만들지 않고 draft를 갱신한다.

### G-5. 문서 분석 포맷 확장

목표:

- PDF/TXT 외 Markdown, DOCX를 우선 지원한다.
- OCR/HWP는 별도 확장으로 분리한다.

작업:

1. `DocumentFileInspector`와 `ResourceAnalysisPublicService.extract(...)` 확장
2. Markdown은 text 계열로 처리
3. DOCX는 Apache POI 또는 현재 의존성 검토 후 도입
4. MIME/확장자 검증 추가
5. 분석 실패 시 사용자 메시지 정리
6. fixture 기반 테스트 추가

완료 기준:

- Markdown, DOCX 업로드 후 분석 job이 성공한다.
- 지원하지 않는 포맷은 `RESOURCE_415_001` 계열로 명확히 실패한다.

### G-6. 문서 초안 확정 저장

목표:

- `DRAFT_DOCUMENT`가 후보에만 머무르지 않고 승인 후 조회 가능한 문서 결과로 남게 한다.

작업:

1. 저장 위치 결정
   - 1안: `ai_documents`에 generated draft 필드/타입 확장
   - 2안: `generated_documents` 신규 테이블
   - 3안: resource version으로 저장
2. API 결정
   - 문서 초안 목록 조회
   - 문서 초안 상세 조회
   - 승인/확정 후 다운로드 또는 export
3. `AgentSuggestionDomainApplyService`에 `DOCUMENT_DRAFT` apply handler 추가
4. 중복 승인 idempotency 처리
5. 테스트 추가

완료 기준:

- 문서 초안 후보 승인 후 별도 API로 결과를 다시 조회할 수 있다.
- 같은 suggestion을 반복 승인해도 문서가 중복 생성되지 않는다.

### G-7. 후보 타입 도메인 반영 확장

목표:

- 보존 처리 중인 후보 타입의 최종 정책을 명확히 한다.

작업:

1. `REQUIREMENT`
   - 별도 requirement 테이블을 만들지 않으면 approved suggestion 자체를 확정 결과로 정의
   - 프로젝트룸 참고 정보와 연결 가능한 필드는 project room update 후보로 분리
2. `QUESTION`, `REVIEW_ITEM`
   - 확인 필요 항목 목록 API 제공
   - 프로젝트룸 이벤트/알림 발행
3. `CONTRACT_FIELD`, `CONTRACT_REVIEW`
   - 계약 금액, 납기, 지급 조건 등 project room 참고값 반영 여부 결정
   - 법률 판단 금지 문구 유지
4. `MEMO`
   - 메모 API 구현 후 승인 시 memo 생성
5. `AgentSuggestionDomainApplyService` handler 분리

완료 기준:

- 모든 suggestion type은 승인 후 `appliedResult.targetType`이 명확하다.
- 반영하지 않는 타입도 "확정 보존" 정책으로 조회 가능하다.

### G-8. 채팅 에이전트 SUGGEST 연결

목표:

- 프로젝트룸 채팅에서 에이전트가 답변뿐 아니라 후보도 만들 수 있게 한다.

작업:

1. `ProjectRoomAgentCommandService`에서 `SUGGEST` 모드일 때 `AgentSuggestion` 생성
2. 생성 타입 정책
   - TODO 후보
   - REVIEW_ITEM 후보
   - QUESTION 후보
3. 응답 메시지 body에 suggestion id 포함
4. 프로젝트룸 이벤트와 알림 발행
5. 테스트 추가

완료 기준:

- `SUGGEST` 명령 실행 후 프로젝트룸 제안함에서 후보를 조회할 수 있다.
- 생성 후보는 바로 확정 데이터가 되지 않는다.

### G-9. 메모 기능 구현

목표:

- 개인 메모와 프로젝트룸 메모 CRUD를 구현한다.

작업:

1. `MemoRepository`, `MemoService`, `MemoController` 추가
2. API
   - 개인 메모 목록/생성/수정/삭제
   - 프로젝트룸 메모 목록/생성/수정/삭제
3. 프로젝트룸 메모 권한 검증
4. 하루정리 context 연결
5. `MEMO` suggestion 승인 연동

완료 기준:

- 개인 메모는 작성자만 접근한다.
- 프로젝트룸 메모는 active member만 접근한다.
- 삭제는 `MemoStatus.DELETED`로 처리한다.

### G-10. 이벤트 발행 보강

목표:

- API 설계서의 project room event와 실제 도메인 변경이 맞게 연결된다.

작업:

1. 이벤트 발행 누락 지점 점검
   - resource upload/update/delete
   - resource analysis start/success/failure
   - task/wbs/schedule 변경
   - agent job/suggestion 변경
   - voice room/participant 변경
2. `ProjectRoomEventRecorder` 사용 범위 확장
3. WebSocket publish와 DB event 저장의 일관성 검증
4. afterSequence 보충 조회 테스트 강화

완료 기준:

- 주요 화면 갱신 이벤트가 DB와 WebSocket 양쪽에 남는다.
- WebSocket 끊김 후 `GET /api/project-rooms/{roomId}/events?afterSequence=...`로 보충 가능하다.

### G-11. 대시보드 조합 데이터 확장

목표:

- `GET /api/dashboard/work`가 실제 작업 시작 화면으로 쓸 수 있게 한다.

작업:

1. 현재 TODO, 일정, 알림, 타이머 중심 응답 확장
2. 추가 후보
   - 프로젝트룸별 진행 요약
   - 보류/검토 필요 agent suggestions
   - 최근 자료 분석 완료
   - 메모 요약
3. 프론트 카드 구성과 충돌하지 않게 optional field로 확장
4. 테스트 추가

완료 기준:

- 사용자의 오늘 업무, 가까운 마감, 확인 필요 항목이 한 응답에 포함된다.

### G-12. 운영 로그와 비용 추적 개선

목표:

- 모델 호출 로그가 실제 운영 분석에 쓸 수 있는 수준이 되게 한다.

작업:

1. Bedrock/Spring AI 응답에서 사용량 metadata 획득 가능 여부 확인
2. 실제 input/output token 기록
3. 비용 계산은 설정 기반 단가로 별도 계산
4. provider error, invalid output, retry 횟수 분리 기록
5. Prometheus metric 추가 검토

완료 기준:

- `agent_model_call_logs`가 추정값이 아닌 실제 token 또는 명시적 unavailable 상태를 기록한다.

## 4. 권장 구현 순서

1. G-1 위젯 API
2. G-3 활동 기록 API
3. G-9 메모 기능
4. G-4 하루정리 context 고도화
5. G-2 로컬 동기화 API
6. G-5 문서 분석 포맷 확장
7. G-6 문서 초안 확정 저장
8. G-7 후보 타입 도메인 반영 확장
9. G-8 채팅 에이전트 SUGGEST 연결
10. G-10 이벤트 발행 보강
11. G-11 대시보드 조합 데이터 확장
12. G-12 운영 로그와 비용 추적 개선

## 5. 검증 계획

공통 검증:

```powershell
.\gradlew.bat test --console=plain
```

기능별 검증:

- 위젯: `docs/http/widget.http` 추가
- 로컬 동기화: `docs/http/localsync.http` 추가
- 활동 기록: `docs/http/personal.http`에 activity 시나리오 추가
- AI/RAG: `docs/http/agent.http`, `docs/RAG_POSTMAN_E2E.md` 갱신
- 메모: `docs/http/personal.http` 또는 `docs/http/memo.http` 추가

최종 E2E:

1. 로그인
2. 프로젝트룸 생성
3. 자료 업로드와 AI 분석
4. 후보 생성, 수정, 승인
5. TODO/WBS/일정/메모/문서 초안 반영 확인
6. 채팅 에이전트 명령 실행
7. 위젯 summary 조회
8. 하루정리 생성과 승인
9. WebSocket/이벤트 보충 확인
10. 권한 실패 케이스 확인

## 6. 제외 또는 후순위

- 보이스챗 녹음
- 음성 자동 회의록
- HWP 고품질 파싱
- 이미지 OCR 고도화
- 세금 계산, 정산 자동화, 수익 분석
- Tauri 로컬 SQLite 내부 테이블의 서버 JPA 매핑

