# API Design

## 14. API 설계

API 응답 형식은 팀 공통으로 맞춘다.

이 문서는 HTTP 경로, 요청 DTO, 응답 DTO, WebSocket payload만 다룬다. 테이블, 컬럼, 인덱스, token hash 저장, sequence 저장 기준은 [Data Model](./09_Data-Model)에 둔다.

API JSON 필드는 `camelCase`를 기본으로 쓴다. DB 컬럼과 로컬 SQLite 컬럼은 `snake_case`를 쓸 수 있지만, 외부로 내려가는 응답과 프론트 요청 본문은 같은 이름 규칙을 유지한다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 시에는 아래처럼 내려준다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "자료를 찾을 수 없습니다.",
    "traceId": "..."
  }
}
```

공통 응답은 아래 구조를 기준으로 둔다.

```ts
type ApiResponse<T> =
  | { success: true; data: T; error: null }
  | { success: false; data: null; error: ApiError };

type ApiError = {
  code: string;
  message: string;
  traceId: string;
  fields?: ApiFieldError[];
};

type ApiFieldError = {
  field: string;
  reason: string;
};
```

일반 목록은 페이지 번호와 전체 개수를 함께 내려준다.

```ts
type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};
```

채팅, 프로젝트룸 이벤트처럼 순서가 중요한 목록은 sequence 기준으로 내려준다.

```ts
type SequenceListResponse<T> = {
  items: T[];
  lastReceivedSequence: number | null;
  latestSequence: number;
  hasNext: boolean;
};

type ChatMessageListResponse = {
  messages: ChatMessageResponse[];
  oldestSequence: number | null;
  lastReceivedSequence: number | null;
  latestRoomSequence: number;
  hasPrevious: boolean;
  hasNext: boolean;
};
```

실시간 이벤트는 WebSocket과 이벤트 보충 API에서 같은 envelope를 쓴다.

```ts
type RealtimeEnvelope<T> = {
  eventId: string;
  eventType: string;
  sequence?: number;
  roomId?: string;
  chatRoomId?: string;
  occurredAt: string;
  actor?: {
    type: 'USER' | 'SYSTEM' | 'AGENT';
    id: string | null;
    name: string;
  };
  payload: T;
};
```

상태값 enum은 Data Model의 데이터 딕셔너리를 기준으로 둔다. API DTO에 개별 enum이 따로 적혀 있지 않더라도 아래 값을 사용한다.

```ts
type AccountStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN';
type AuthSessionStatus = 'ACTIVE' | 'REVOKED' | 'EXPIRED';
type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELED';
type ProjectRoomStatus = 'ACTIVE' | 'CLOSED';
type PaymentStatus = 'NOT_RECORDED' | 'PENDING' | 'PAID' | 'OVERDUE';
type RoomMemberStatus = 'ACTIVE' | 'LEFT' | 'REMOVED';
type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELED';
type ResourceStatus = 'UPLOADING' | 'READY' | 'ANALYZING' | 'ANALYZED' | 'FAILED';
type ResourceSummaryStatus = 'READY' | 'ANALYZING' | 'ANALYZED' | 'FAILED';
type AiDocumentStatus = 'READY' | 'ANALYZING' | 'ANALYZED' | 'FAILED';
type AgentJobStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELED';
type AgentSuggestionStatus = 'DRAFT' | 'APPROVED' | 'HELD' | 'REJECTED';
type ApprovedSummaryStatus = 'DRAFT' | 'APPROVED';
type WbsStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'REVIEW' | 'DONE' | 'BLOCKED';
type ScheduleSyncStatus = 'LOCAL_ONLY' | 'SYNCED' | 'SYNC_FAILED';
type MemoStatus = 'ACTIVE' | 'DELETED';
type TimerStatus = 'RUNNING' | 'PAUSED' | 'ENDED' | 'NEEDS_RECOVERY';
type ChatRoomStatus = 'ACTIVE' | 'CLOSED';
type ChatRoomMemberStatus = 'ACTIVE' | 'LEFT';
type NotificationStatus = 'UNREAD' | 'READ';
type VoiceRoomStatus = 'OPEN' | 'ENDED';
type VoiceParticipantStatus = 'JOINED' | 'LEFT' | 'DISCONNECTED';
type ManagedFolderStatus = 'ACTIVE' | 'PAUSED' | 'REMOVED';
type LocalFileSyncStatus = 'LOCAL_ONLY' | 'SYNC_PENDING' | 'SYNCED' | 'CONFLICT';
type SyncJobStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';
type LocalChatCacheStatus = 'VALID' | 'STALE' | 'REBUILDING';
type WidgetItemState = 'VISIBLE' | 'CONFIRMED' | 'HIDDEN' | 'PINNED' | 'SNOOZED';
type WidgetUsageSyncStatus = 'LOCAL_ONLY' | 'SYNC_PENDING' | 'SYNCED' | 'FAILED';
type LocalTimerStatus = 'RUNNING' | 'PAUSED' | 'NEEDS_RECOVERY';
type LocalSyncQueueStatus = 'PENDING' | 'SENDING' | 'SENT' | 'FAILED';
```

상태 row 자체가 아직 없어서 상태를 판단할 수 없는 경우에는 API 전용 enum 값을 만들지 않고 `null` 또는 필드 생략으로 표현한다.

단, 화면 표시나 집계 응답 때문에 API 전용 상태값이 꼭 필요하면 DB 상태값과 섞지 않고 별도 타입으로 정의한다. 이때 타입명은 `Api` 접두사를 붙이고, 각 API 전용 값이 어떤 상황에서 내려가는지 반드시 함께 적는다.

```ts
type ApiResourceSummaryStatus =
  | ResourceSummaryStatus
  | 'NONE'; // resource_summaries row가 아직 없어서 요약 상태 자체가 없는 경우에만 사용
```

API 전용 상태값은 DB에 저장하지 않는다. 서버 내부 저장, 검색, 상태 전이 판단은 Data Model의 상태값을 기준으로 하고, API 전용 값은 응답을 만들 때만 계산한다.

### 14.1 공개 사이트와 회원 앱 라우트

공개 사이트와 회원 웹 앱은 주소를 분리한다. 공개 사이트는 비회원에게 열려 있고, 회원 웹 앱은 로그인 후에만 들어간다.

|구분|경로 예시|설명|
|---|---|---|
|공개 사이트|GET /|서비스 소개 첫 화면|
|공개 사이트|GET /features|기능 안내|
|공개 사이트|GET /download|macOS와 Windows 데스크탑 앱 다운로드|
|공개 사이트|GET /faq|자주 묻는 질문|
|공개 사이트|GET /login|로그인 진입|
|회원 웹 앱|GET /app|로그인 후 대시보드|
|회원 웹 앱|GET /app/project-rooms|프로젝트룸 목록|
|회원 웹 앱|GET /app/resources|자료보드|
|회원 웹 앱|GET /app/project-rooms/{roomId}/work|프로젝트룸 WBS/작업판|
|회원 웹 앱|GET /app/chat|채널|
|Tauri 앱|WebView /app|로그인 후 회원 웹 앱을 앱 창으로 표시|

인증 API는 구글 소셜 로그인만 사용한다. 로컬 회원가입과 비밀번호 로그인은 만들지 않는다. 서버는 구글 OAuth의 `google_sub`로 사용자를 식별하고, 최초 로그인 시 `users`를 자동 생성한다.

웹과 Tauri 앱은 같은 서버 인증 API를 쓰되, refresh token 저장 위치만 다르게 둔다. access token은 30분, refresh token은 30일을 기본값으로 잡는다. 웹은 refresh token을 httpOnly, Secure, SameSite=Lax cookie에 저장하고, Tauri 앱은 운영체제 보안 저장소에 저장한다. 서버는 refresh token 원문을 저장하지 않고 hash와 기기 세션 기준으로 검증한다.

|메서드|경로|설명|
|---|---|---|
|GET|/api/auth/google/authorize|구글 OAuth 로그인 시작 URL 발급 또는 redirect|
|POST|/api/auth/google/callback|구글 OAuth code 검증, 사용자 생성 또는 조회, token 발급|
|POST|/api/auth/refresh|access token 재발급. refresh token rotation 적용|
|POST|/api/auth/logout|현재 기기 세션 로그아웃|
|GET|/api/me|내 프로필 조회|
|PATCH|/api/me|내 정보 수정|

웹 로그인 응답은 refresh token을 본문에 넣지 않는다. Tauri 로그인 응답은 운영체제 보안 저장소에 넣을 refresh token을 함께 받는다.

```ts
type LoginResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  expiresAt: string;
  user: {
    id: string;
    bubliId: string;
    name: string;
    avatarUrl?: string | null;
    locale?: string | null;
    timezone?: string | null;
  };
};

type TauriLoginResponse = LoginResponse & {
  refreshToken: string;
  refreshTokenExpiresAt: string;
};

type RefreshResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  expiresAt: string;
};

type TauriRefreshResponse = RefreshResponse & {
  refreshToken: string;
  refreshTokenExpiresAt: string;
};
```

인증 에러 코드는 아래 기준을 쓴다.

|상황|HTTP|코드|
|---|---:|---|
|구글 OAuth code 검증 실패|401|AUTH_OAUTH_INVALID_CODE|
|구글 계정 식별값 없음|401|AUTH_OAUTH_SUB_MISSING|
|지원하지 않는 로그인 제공자|400|AUTH_UNSUPPORTED_PROVIDER|
|access token 만료|401|AUTH_TOKEN_EXPIRED|
|access token 위변조 또는 형식 오류|401|AUTH_INVALID_TOKEN|
|refresh token 만료|401|AUTH_REFRESH_TOKEN_EXPIRED|
|refresh token 재사용 감지|401|AUTH_REFRESH_TOKEN_REUSED|
|로그인 필요|401|AUTH_UNAUTHENTICATED|
|권한 없음|403|AUTH_FORBIDDEN|

프론트는 `AUTH_TOKEN_EXPIRED`를 받으면 즉시 로그아웃하지 않고 `/api/auth/refresh`를 먼저 시도한다.

### 14.2 프로젝트룸, 멤버, 자료 API

2026-06-24 DB 회의 반영본 기준으로 `projects`와 `project_rooms`는 분리하지 않는다. 프로젝트 생성, 수정, 계약/입금 참고값, 멤버 권한은 모두 프로젝트룸 기준으로 다룬다. 따라서 신규 API는 `/api/projects/*`를 만들지 않고 `/api/project-rooms/*`로 통일한다.

프로젝트룸 권한은 `room_members`를 기준으로 판단한다. 프로젝트룸 자료는 `resources.roomId`와 `room_members` 기준으로 접근을 확인하고, 개인 자료는 `resources.ownerId` 기준으로 확인한다.

|메서드|경로|설명|
|---|---|---|
|GET|/api/project-rooms|내가 만든 프로젝트룸과 참여 중인 프로젝트룸 목록|
|POST|/api/project-rooms|프로젝트룸 생성. 생성자는 `room_members`에 `LEADER`로 함께 등록|
|GET|/api/project-rooms/{roomId}|프로젝트룸 상세|
|PATCH|/api/project-rooms/{roomId}|프로젝트룸 이름, 설명, 클라이언트명, 상태 수정|
|PATCH|/api/project-rooms/{roomId}/payment|계약 금액, 입금 상태, 입금 예정일, 실제 입금일 수정|
|DELETE|/api/project-rooms/{roomId}|프로젝트룸 종료 처리. `status=CLOSED`, `closedAt` 기록|
|POST|/api/project-rooms/{roomId}/contract-documents|계약서, 요구사항 문서 자료 업로드와 분석 작업 생성|
|GET|/api/project-rooms/{roomId}/ai-documents|프로젝트룸에 연결된 AI 문서 분석 목록|
|GET|/api/project-rooms/{roomId}/members|프로젝트룸 멤버 목록|
|POST|/api/project-rooms/{roomId}/invitations|Bubli ID 또는 가입 사용자 ID로 프로젝트룸 멤버 초대|
|GET|/api/project-rooms/{roomId}/invitations|프로젝트룸 초대 목록과 상태 조회|
|PATCH|/api/invitations/{id}/accept|초대 수락|
|PATCH|/api/invitations/{id}/cancel|초대 취소|
|PATCH|/api/project-rooms/{roomId}/members/{userId}|멤버 역할 변경|
|DELETE|/api/project-rooms/{roomId}/members/{userId}|멤버 나가기 또는 내보내기|
|GET|/api/resources?scope=personal|개인 자료 목록|
|GET|/api/project-rooms/{roomId}/resources|프로젝트룸 자료 목록|
|POST|/api/resources|개인 또는 프로젝트룸 자료 업로드. `visibility`가 `ROOM_SHARED`면 `roomId` 필수|
|GET|/api/resources/{id}|자료 상세|
|PATCH|/api/resources/{id}|자료 제목, 공개 범위, 상태 수정|
|GET|/api/resources/{id}/download-url|권한 확인 후 다운로드 주소 발급|
|GET|/api/resources/{id}/summary|자료 요약 결과|
|GET|/api/resources/{id}/ai-document|자료의 AI 문서 분류와 분석 상태|
|GET|/api/resources/{id}/related|관련 문서|
|GET|/api/resources/{id}/versions|버전 목록|
|POST|/api/resources/{id}/versions|새 버전 등록|
|GET|/api/resources/{id}/comments|자료 댓글 목록|
|POST|/api/resources/{id}/comments|자료 댓글 작성|
|PATCH|/api/resource-comments/{id}|자료 댓글 수정|
|DELETE|/api/resource-comments/{id}|자료 댓글 삭제 처리|
|DELETE|/api/resources/{id}|자료 삭제 처리. 복구 API는 두지 않음|

프로젝트룸 생성/수정 요청은 `project_rooms` 컬럼과 맞춘다.

```ts
type ProjectRoomUpsertRequest = {
  name: string;
  clientName?: string | null;
  contractAmount?: number | null;
  paymentStatus?: PaymentStatus;
  paymentDueDate?: string | null;
  paidAt?: string | null;
  status?: ProjectRoomStatus;
};
```

자료 응답은 자료 카드와 실제 파일 메타데이터를 분리해서 내려준다. 파일 교체는 기존 `resourceFile` 수정이 아니라 새 `resourceFile`과 새 `resourceVersion` 생성으로 처리한다.

```ts
type ResourceResponse = {
  id: string;
  ownerId: string;
  roomId?: string | null;
  title: string;
  kind: 'FILE' | 'MEMO';
  visibility: 'PERSONAL' | 'ROOM_SHARED';
  status: ResourceStatus;
  currentVersion?: ResourceVersionResponse | null;
  summaryStatus?: ResourceSummaryStatus | null;
  aiDocumentStatus?: AiDocumentStatus | null;
  createdAt: string;
  updatedAt: string;
};

type ResourceFileResponse = {
  id: string;
  resourceId: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
  checksum?: string | null;
  createdAt: string;
};

type ResourceVersionResponse = {
  id: string;
  resourceId: string;
  versionNo: number;
  file: ResourceFileResponse;
  createdBy: string;
  createdAt: string;
};
```

### 14.3 에이전트와 작업 API

에이전트 실행 API는 결과를 즉시 반환하지 않고 `jobId`를 반환할 수 있다. API 서버는 로그인, 권한, 분석 제한을 확인한 뒤 `agent_jobs`를 만들고, 에이전트 모듈이 작업을 처리한다. 프론트와 Tauri 앱은 `GET /api/agent-jobs/{jobId}` 또는 WebSocket 이벤트로 상태를 확인한다. 에이전트 모듈이 별도 컨테이너로 분리되더라도 프론트와 Tauri 앱의 호출 경로는 API 서버로 유지한다.

에이전트 작업 생성 응답은 아래 필드를 기준으로 한다.

|필드|설명|
|---|---|
|jobId|에이전트 작업 ID|
|status|PENDING, RUNNING, SUCCEEDED, FAILED, CANCELED|
|targetType|RESOURCE, PROJECT_ROOM, DAILY_SUMMARY 같은 작업 대상 종류|
|targetId|작업 대상 ID|
|errorCode|실패 시 오류 코드|
|errorMessage|사용자에게 보여줄 수 있는 실패 설명|
|retryable|사용자가 다시 시도할 수 있는 실패인지 여부|
|suggestionIds|완료 후 생성된 후보 ID 목록|
|resourceSummaryId|완료 후 생성된 자료 요약 ID|
|aiDocumentId|완료 후 생성 또는 갱신된 AI 문서 ID|

|메서드|경로|설명|
|---|---|---|
|POST|/api/ai/analyze-resource|자료 요약, 임베딩, AI 문서 분류 작업 생성. `jobId` 반환 가능|
|POST|/api/ai/generate-requirements|요구사항 후보 생성 작업 생성. `jobId` 반환 가능|
|POST|/api/ai/generate-tasks|TODO 후보 생성 작업 생성. 후보는 `agent_suggestions`로 저장|
|POST|/api/ai/generate-wbs|WBS 후보 생성 작업 생성. `jobId` 반환 가능|
|POST|/api/ai/review-contract-documents|계약서와 요구사항 문서 추출, 비교, 확인 질문 생성 작업. 결과 후보는 `agent_suggestions`로 저장|
|POST|/api/ai/generate-questions|확인 질문 후보 생성 작업 생성. `jobId` 반환 가능|
|POST|/api/ai/summarize-day|개인 하루정리 요약 후보 생성 작업 생성. 개인 에이전트 원문 저장 금지|
|GET|/api/agent-jobs/{jobId}|에이전트 작업 상태, 실패 사유, 결과 연결 정보 조회|
|GET|/api/agent-jobs/{jobId}/events|에이전트 작업 상태 전이와 처리 이벤트 조회|
|GET|/api/daily-summaries|사용자가 확인한 개인 하루정리 요약 조회|
|PATCH|/api/daily-summaries/{id}|개인 하루정리 요약 승인, 수정, 보류|
|POST|/api/ai/search-resource|자료 의미 검색|
|POST|/api/ai/draft-document|문서 초안 생성|
|GET|/api/agent/suggestions|개인 제안함|
|GET|/api/project-rooms/{roomId}/agent/suggestions|프로젝트룸 제안함|
|PATCH|/api/agent/suggestions/{id}|요구사항, WBS, TODO, 문서 초안, 문서 추출값, 확인 질문 후보 승인, 수정, 보류, 삭제|
|GET|/api/tasks?scope=personal|개인 TODO 목록|
|POST|/api/tasks|개인 TODO 생성|
|PATCH|/api/tasks/{id}|업무 제목, 담당자, 마감일, 상태 수정|
|DELETE|/api/tasks/{id}|업무 삭제|
|GET|/api/dashboard/tasks|대시보드에 표시할 내 TODO 목록|
|GET|/api/project-rooms/{roomId}/tasks|프로젝트룸 TODO 목록|
|POST|/api/project-rooms/{roomId}/tasks|프로젝트룸 TODO 생성|
|POST|/api/time-logs/start|타이머 시작과 서버 time_logs 생성. idempotencyKey로 중복 시작 방지|
|PATCH|/api/time-logs/{id}/pause|타이머 일시정지|
|PATCH|/api/time-logs/{id}/resume|타이머 재개|
|PATCH|/api/time-logs/{id}/stop|타이머 종료와 작업 시간 확정. durationSeconds 확정|
|PATCH|/api/time-logs/{id}/heartbeat|실행 중 타이머 마지막 확인 시각 갱신. 기본 60초 주기|
|GET|/api/project-rooms/{roomId}/wbs-board|프로젝트룸 WBS/작업판 데이터|
|GET|/api/project-rooms/{roomId}/wbs-items|프로젝트룸 WBS 목록|
|POST|/api/project-rooms/{roomId}/wbs-items|WBS 항목 생성|
|PATCH|/api/project-rooms/{roomId}/wbs-items/reorder|WBS 항목 순서 변경|
|PATCH|/api/wbs-items/{id}|WBS 항목 수정|
|DELETE|/api/wbs-items/{id}|WBS 항목 삭제|
|GET|/api/dashboard/work|사용자 기준 대시보드|
|GET|/api/widget/summary|위젯 버블 표시 데이터|

계약 문서 추출값, 확인 질문, 요구사항 후보, TODO 후보, 문서 초안은 별도 후보 테이블을 만들지 않고 `agent_suggestions`로 통합한다. 프론트는 `suggestionType`으로 화면과 승인 동작을 구분한다. 문서 초안은 `suggestionType = DOCUMENT_DRAFT`로 둔다.

### 14.4 채팅, 알림, 일정, 위젯 API

채팅 메시지는 서버 DB의 `chat_messages`를 원본으로 둔다. 클라이언트는 메시지를 보낼 때 `clientMessageId`를 함께 보내고, 서버는 저장이 끝난 메시지에 `roomSequence`를 부여한 뒤 WebSocket으로 알린다. WebSocket이 끊긴 클라이언트는 `afterSequence`로 빠진 메시지나 이벤트를 다시 받는다.

|계약 항목|기준|
|---|---|
|중복 전송 방지|`chatRoomId + clientMessageId` 기준으로 같은 메시지를 두 번 저장하지 않는다|
|메시지 순서|서버가 `roomSequence`를 발급하고, 화면 정렬과 캐시 동기화는 이 값을 따른다|
|읽음 처리|`lastReadSequence`를 서버에 보내 채팅방 읽음 위치를 저장한다|
|이전 메시지 조회|`beforeSequence`로 과거 메시지를 조회한다|
|빠진 메시지 조회|`afterSequence`로 WebSocket 끊김 중 빠진 메시지를 보충한다|

채팅 메시지 응답은 아래 구조를 기준으로 한다.

```ts
type ChatMessageResponse = {
  id: string;
  chatRoomId: string;
  sender: {
    type: 'USER' | 'AGENT' | 'SYSTEM';
    id: string | null;
    name: string;
  };
  messageType: 'TEXT' | 'FILE' | 'AGENT_COMMAND' | 'AGENT_RESPONSE' | 'SYSTEM';
  body: Record<string, unknown>;
  clientMessageId?: string;
  roomSequence: number;
  createdAt: string;
};
```

|메서드|경로|설명|
|---|---|---|
|GET|/api/friends|내 친구 목록|
|GET|/api/friends/search?bubliId={id}|Bubli ID로 사용자 검색|
|GET|/api/friend-requests|받은 친구 요청과 보낸 친구 요청 목록|
|POST|/api/friend-requests|친구 요청 보내기|
|PATCH|/api/friend-requests/{id}/accept|친구 요청 수락|
|PATCH|/api/friend-requests/{id}/reject|친구 요청 거절|
|DELETE|/api/friends/{friendUserId}|친구 삭제. 차단 상태는 만들지 않음|
|GET|/api/chat/rooms|내가 참여한 채팅방 목록|
|POST|/api/chat/direct-rooms|친구와 1:1 채팅방 생성 또는 기존 방 조회|
|GET|/api/chat/rooms/{id}/messages|채팅 메시지 목록. afterSequence로 빠진 메시지 조회 가능|
|POST|/api/chat/rooms/{id}/messages|채팅 메시지 전송. clientMessageId로 중복 저장 방지|
|PATCH|/api/chat/rooms/{id}/read|채팅방 읽음 처리|
|POST|/api/project-rooms/{roomId}/agent-command|프로젝트룸 채팅 명령어 실행|
|POST|/api/project-rooms/{roomId}/memory-summaries|프로젝트룸 장기기억 요약 생성|
|GET|/api/project-rooms/{roomId}/memory-summaries|프로젝트룸 장기기억 요약 조회|
|GET|/api/notifications|읽지 않은 알림과 최근 알림 조회|
|PATCH|/api/notifications/{id}/read|알림 읽음 처리|
|GET|/api/schedules|내 일정 목록|
|POST|/api/schedules|내부 일정 생성|
|PATCH|/api/schedules/{id}|일정 수정|
|DELETE|/api/schedules/{id}|일정 삭제|
|GET|/api/widget/settings|버블 활성 상태, 배치, 표시 범위 조회|
|PATCH|/api/widget/settings|버블 활성 상태, 배치, 표시 범위 저장|
|GET|/api/widget/context|위젯 전체가 바라보는 선택 프로젝트룸 조회. null이면 개인 모드|
|PATCH|/api/widget/context|위젯 전체 선택 프로젝트룸 저장|
|PATCH|/api/widget/items/{id}/state|버블 항목 확인, 숨김, 고정, 다시 보기 상태 저장|
|POST|/api/widget/usage-summaries|Tauri에서 만든 날짜별, 기기별 버블 사용 집계 저장. rollupKey 중복 요청은 재집계하지 않음|
|GET|/api/widget/usage-summaries/today|오늘 버블 사용 집계 조회. 여러 기기 집계를 사용자 기준으로 합산|
|GET|/api/project-rooms/{roomId}/events|프로젝트룸 이벤트 목록. afterSequence로 빠진 이벤트 조회 가능|

WebSocket topic은 아래처럼 둔다.

|topic|용도|
|---|---|
|/topic/chat/{chatRoomId}|채팅방 메시지 수신|
|/topic/project-rooms/{roomId}/events|프로젝트룸 자료, 댓글, 에이전트 제안 이벤트|
|/user/queue/notifications|사용자 개인 알림|

프로젝트룸 이벤트는 자료, 댓글, 에이전트 작업 상태, 후보 생성, 멤버 변경, 초대 상태처럼 화면을 갱신해야 하는 일을 전달한다. 채팅 메시지는 `/topic/chat/{chatRoomId}`에서 따로 받고, 프로젝트룸 이벤트는 `/topic/project-rooms/{roomId}/events`에서 받는다.

WebSocket 연결은 STOMP connect header에 access token을 담는다.

```text
Authorization: Bearer <accessToken>
```

access token 만료로 연결이 끊기면 refresh 후 다시 연결하고, 마지막으로 받은 sequence 이후 이벤트를 HTTP API로 보충한다.

프로젝트룸 이벤트 종류는 아래 기준을 쓴다.

|이벤트|발생 시점|프론트 처리|
|---|---|---|
|ROOM_UPDATED|프로젝트룸 이름, 설명, 설정 변경|룸 헤더와 설정 갱신|
|ROOM_MEMBER_JOINED|멤버가 프로젝트룸에 참여|멤버 목록 갱신|
|ROOM_MEMBER_LEFT|멤버가 나감|멤버 목록 갱신|
|ROOM_MEMBER_ROLE_CHANGED|멤버 권한 변경|멤버 목록과 권한 UI 갱신|
|ROOM_MEMBER_REMOVED|멤버가 내보내짐|멤버 목록 갱신, 대상자는 접근 차단|
|RESOURCE_UPLOADED|자료가 업로드됨|자료 목록 갱신|
|RESOURCE_UPDATED|자료 제목 또는 메타데이터 변경|자료 카드 갱신|
|RESOURCE_DELETED|자료 삭제|자료 목록에서 제거|
|RESOURCE_ANALYSIS_STARTED|자료 분석 시작|분석 상태 표시|
|RESOURCE_ANALYSIS_COMPLETED|자료 분석 완료|분석 결과와 요약 갱신|
|RESOURCE_ANALYSIS_FAILED|자료 분석 실패|실패 상태 표시|
|TASK_CREATED|TODO 생성|TODO 목록 갱신|
|TASK_UPDATED|TODO 제목, 담당자, 기한 변경|TODO 카드 갱신|
|TASK_STATUS_CHANGED|TODO 상태 변경|칸반과 목록 상태 갱신|
|TASK_DELETED|TODO 삭제|목록에서 제거|
|WBS_CREATED|WBS 항목 생성|WBS 트리 갱신|
|WBS_UPDATED|WBS 항목 수정|WBS 트리 갱신|
|WBS_REORDERED|WBS 순서 변경|WBS 순서 갱신|
|WBS_DELETED|WBS 삭제|WBS 트리에서 제거|
|SCHEDULE_CREATED|내부 일정 생성|캘린더 갱신|
|SCHEDULE_UPDATED|내부 일정 수정|캘린더 갱신|
|SCHEDULE_DELETED|내부 일정 삭제|캘린더에서 제거|
|AGENT_JOB_CREATED|에이전트 작업 생성|에이전트 상태 표시 시작|
|AGENT_JOB_STATUS_CHANGED|에이전트 작업 상태 변경|진행률과 상태 UI 갱신|
|AGENT_SUGGESTIONS_CREATED|에이전트 후보 생성 완료|후보 목록 갱신|
|AGENT_SUGGESTION_APPROVED|후보 승인됨|TODO, WBS, 자료, 메모 갱신|
|AGENT_SUGGESTION_REJECTED|후보 거절됨|후보 목록 갱신|
|VOICE_ROOM_OPENED|보이스챗 방 생성 또는 시작|보이스 참여 버튼 활성화|
|VOICE_PARTICIPANT_JOINED|참여자가 입장|참여자 목록 갱신|
|VOICE_PARTICIPANT_LEFT|참여자가 퇴장|참여자 목록 갱신|
|VOICE_ROOM_ENDED|보이스챗 종료|보이스 UI 종료 상태로 변경|

프로젝트룸 이벤트 payload는 아래 envelope를 쓴다.

```ts
type ProjectRoomEventEnvelope<T> = {
  eventId: string;
  eventType: string;
  roomId: string;
  sequence: number;
  occurredAt: string;
  actor: {
    type: 'USER' | 'SYSTEM' | 'AGENT';
    id: string | null;
    name: string;
  };
  payload: T;
};
```

누락된 프로젝트룸 이벤트는 아래 API로 보충한다.

```http
GET /api/project-rooms/{roomId}/events?afterSequence={lastReceivedSequence}&limit=100
```

### 14.5 사용자별 설정 API

사용자별 설정 API는 프로젝트룸 설정과 분리한다. 같은 프로젝트룸에 있어도 사용자마다 테마, 기본 시작 화면, 기본 프로젝트룸, 알림, 위젯, 활동 감지 동의가 다를 수 있다.

|메서드|경로|설명|
|---|---|---|
|GET|/api/me|내 프로필 조회|
|PATCH|/api/me|내 이름, 프로필 이미지, 언어, 시간대 수정|
|GET|/api/me/preferences|테마, 기본 홈, 기본 프로젝트룸 조회|
|PATCH|/api/me/preferences|테마, 기본 홈, 기본 프로젝트룸 저장|
|GET|/api/me/notification-preferences|사용자별 알림 설정 조회|
|PATCH|/api/me/notification-preferences|메시지, 댓글, 자료 버전, 에이전트, 용량 초과 알림 On/Off 저장|
|GET|/api/me/privacy-consents|활동 감지와 로컬 기능 동의 상태 조회|
|PATCH|/api/me/privacy-consents|활동 감지와 로컬 기능 동의 상태 변경|
|GET|/api/me/project-rooms|내가 만든 프로젝트룸과 참여 중인 프로젝트룸 목록|

언어와 시간대는 `users.locale`, `users.timezone`에 두고, 기본 홈은 `user_preferences.defaultHomeType`, 기본 프로젝트룸은 `user_preferences.defaultProjectRoomId`로 내려준다. 위젯 화면 전체가 현재 바라보는 프로젝트룸은 사용자 기본값과 분리해서 `/api/widget/context`에서 관리한다.

### 14.6 Tauri 전용 API

Tauri 전용 기능은 둘로 나눈다. 로컬 파일 접근, 폴더 선택, 파일 감시, SQLite 색인은 Tauri IPC가 맡는다. 서버에 반영되는 일만 HTTP API를 쓴다.

|Tauri IPC|설명|
|---|---|
|select_managed_folder|사용자가 선택한 폴더 권한을 얻고 로컬 설정에 저장|
|scan_managed_folder|선택 폴더의 파일명, 경로, 크기, 수정일, hash를 다시 확인|
|watch_managed_folder|앱 실행 중 파일 추가, 수정, 삭제, 이동 감지|
|search_local_files|로컬 SQLite/FTS5 기준 파일명과 일부 추출 텍스트 검색|
|sync_local_file_changes|개인 자료함 동기화가 켜진 로컬 변경분을 백엔드 localsync API로 전송|
|update_local_file_sync_policy|개인 자료함 동기화 대상 여부를 로컬 파일 정책으로 저장|
|sync_room_messages|서버 chat_messages를 Tauri SQLite의 프로젝트룸 채팅 캐시에 반영|
|backup_local_sqlite|개인 에이전트 대화와 로컬 설정을 포함한 SQLite 백업 생성|
|restore_local_sqlite_backup|선택한 로컬 SQLite 백업으로 복구|
|check_local_sqlite_integrity|앱 시작 시 SQLite 무결성 검사|
|read_activity_context|사용자 동의 후 현재 앱 이름, 창 제목, 머문 시간 확인|
|record_widget_usage_event|버블 열기, 닫기, 클릭, 확인 같은 상세 이벤트를 로컬 SQLite에 기록|
|rollup_widget_usage|로컬 위젯 상세 이벤트를 날짜별, 기기별, 버블별 집계로 압축|
|sync_widget_usage_summary|로컬 위젯 집계값을 rollupKey와 함께 서버 widget_daily_summaries에 반영|
|flush_sync_outbox|네트워크 복구 후 local_sync_outbox의 미전송 요청을 idempotencyKey 기준으로 재전송. 실패 시 재시도 간격을 늘림|
|recover_timer_state|앱 시작 시 local_timer_state와 서버 time_logs를 비교해 타이머 복구 상태 확인|

|메서드|경로|설명|
|---|---|---|
|GET|/api/storage/usage|사용자별 서버 저장 용량과 남은 용량 조회|
|POST|/api/local-file-events/sync|Tauri가 감지한 개인 관리 폴더 변경분을 서버 개인 자료함에 자동 반영. 프로젝트룸 자료로 공유하지 않음|
|POST|/api/activity/current-app|현재 앱과 창 제목 기반 작업 맥락 기록|
|GET|/api/activity/today|오늘 작업 맥락 조회|
|DELETE|/api/activity/{id}|활동 기록 삭제|

개인 자료를 프로젝트룸 자료로 자동 공유하거나 이동하는 API는 두지 않는다. 프로젝트룸에 공유하려면 사용자가 해당 프로젝트룸 자료 업로드 흐름에서 직접 올린다. 삭제된 자료 복구 API와 삭제 대신 상태만 바꾸는 처리는 두지 않는다. 자료 삭제는 `DELETE /api/resources/{id}`로 처리한다.

### 14.7 보이스챗 API

LiveKit 접속 토큰은 서버에서만 만든다. 프론트와 Tauri 앱에는 LiveKit key와 secret을 내려주지 않고, 권한 확인이 끝난 뒤 접속에 필요한 짧은 수명의 token만 내려준다. token 응답은 `serverUrl`, `token`, `voiceRoomId`, `participantId`, `expiresAt`을 기준으로 한다.

|메서드|경로|설명|
|---|---|---|
|POST|/api/voice/rooms|프로젝트룸 보이스챗 방 생성. 1:1 보이스는 같은 구조를 재사용하는 확장 후보|
|GET|/api/voice/rooms/{id}|보이스챗 방 상태와 참여자 조회|
|POST|/api/voice/rooms/{id}/token|권한 확인 후 LiveKit 접속 토큰 발급|
|PATCH|/api/voice/rooms/{id}/leave|통화 나가기와 참가 기록 종료|
|PATCH|/api/voice/rooms/{id}/end|프로젝트 리더 등 권한 있는 멤버 또는 서버가 통화 종료 처리|

보이스챗 방 상태는 `OPEN`, `ENDED`를 쓴다. 참가 기록은 `JOINED`, `LEFT`, `DISCONNECTED`를 기준으로 화면에 표시한다.

### 14.8 구현 확인 산출물

프론트와 백엔드가 같은 계약을 보고 구현하기 위해 아래 산출물을 맞춘다.

|산출물|목적|
|---|---|
|auth.http|로그인, refresh, logout 검증|
|Swagger/OpenAPI|요청과 응답 DTO 기준 확인|
|chat.http|채팅 HTTP API 검증|
|WebSocket payload 예시|채팅과 프로젝트룸 이벤트 구현 기준|
|voice.http|LiveKit token 발급 검증|
|agent.http|에이전트 job 생성, 조회, 이벤트 검증|
|widget.http|위젯 rollup 검증|
|personal.http|타이머와 개인 기능 검증|
|localsync.http|Tauri 동기화 검증|
