# 채팅창 `/bubli` 에이전트 호출 개발 계획

작성일: 2026-07-01

## 1. 목표

채팅창에서 사용자가 `/bubli` 명령어를 입력하면 일반 채팅 메시지로 저장하지 않고, 프로젝트룸 에이전트 호출 API로 라우팅한다.

예시:

```text
/bubli 가장 최근에 올라온 계약서 알려줘
```

기대 동작:

1. 프론트엔드가 `/bubli` prefix를 감지한다.
2. prefix를 제거한 사용자 요청을 백엔드 agent command API로 전송한다.
3. 백엔드는 프로젝트룸 컨텍스트와 자료 정보를 기준으로 답변을 생성한다.
4. 에이전트 응답은 프로젝트룸 채팅에 `AGENT_RESPONSE` 메시지로 저장된다.
5. 채팅창은 기존 메시지 목록/웹소켓 갱신 흐름으로 에이전트 응답을 보여준다.

## 2. 현재 사용 가능한 백엔드 기능

이미 존재하는 API:

```http
POST /api/project-rooms/{roomId}/agent/commands
```

요청 body:

```json
{
  "message": "가장 최근에 올라온 계약서 알려줘",
  "mode": "ANSWER",
  "resourceIds": []
}
```

지원 mode:

```text
ANSWER
SUMMARIZE
SUGGEST
```

현재 백엔드는 `ProjectRoomAgentCommandService`를 통해 에이전트 응답을 생성하고, `ChatMessagePublicService.createRoomAgentResponse(...)`를 통해 프로젝트룸 채팅에 `AGENT_RESPONSE` 메시지를 저장한다.

## 3. 프론트엔드 개발 계획

### 3.1 입력 명령어 감지

채팅 전송 handler에서 입력값을 trim한 뒤 `/bubli` 명령어인지 확인한다.

```ts
const text = input.trim();
const isBubliCommand = /^\/bubli(\s|$)/i.test(text);
```

처리 기준:

- `/bubli` 또는 `/bubli ...`만 명령어로 처리한다.
- `/bublitest`, `hello /bubli`는 일반 메시지로 처리한다.
- `/bubli` 뒤 메시지가 비어 있으면 전송하지 않고 안내 문구를 표시한다.

### 3.2 일반 메시지와 에이전트 명령 분기

```ts
async function sendChatMessage(input: string) {
  const text = input.trim();

  if (/^\/bubli(\s|$)/i.test(text)) {
    const message = text.replace(/^\/bubli\s*/i, "").trim();
    if (!message) {
      showToast("Bubli에게 요청할 내용을 입력해 주세요.");
      return;
    }

    await sendBubliCommand(message);
    clearInput();
    return;
  }

  await sendNormalChatMessage(text);
  clearInput();
}
```

### 3.3 Agent command API 호출

```ts
async function sendBubliCommand(message: string) {
  await api.post(`/api/project-rooms/${roomId}/agent/commands`, {
    message,
    mode: resolveAgentMode(message),
    resourceIds: selectedResourceIds,
  });
}
```

초기 구현에서는 `selectedResourceIds`가 없으면 빈 배열을 보낸다.

### 3.4 mode 판별

초기 버전은 단순 규칙 기반으로 처리한다.

```ts
function resolveAgentMode(message: string) {
  if (message.includes("요약")) {
    return "SUMMARIZE";
  }
  if (
    message.includes("제안") ||
    message.includes("TODO") ||
    message.includes("할일") ||
    message.includes("검토 항목")
  ) {
    return "SUGGEST";
  }
  return "ANSWER";
}
```

예시:

| 입력 | mode |
|---|---|
| `/bubli 가장 최근에 올라온 계약서 알려줘` | `ANSWER` |
| `/bubli 이 프로젝트 요약해줘` | `SUMMARIZE` |
| `/bubli 계약서 검토 항목 제안해줘` | `SUGGEST` |

### 3.5 UI 상태

필수 상태:

- 전송 중: 입력창 또는 전송 버튼 disabled
- 실패 시: toast 또는 inline error 표시
- 성공 시: 입력창 초기화

권장 표시:

- 사용자가 입력한 `/bubli ...` 명령을 일반 채팅 메시지로 저장하지 않는다면, 전송 직후 "Bubli가 답변을 준비 중입니다" 임시 메시지를 UI에만 표시한다.
- 백엔드에서 `AGENT_RESPONSE` 메시지가 내려오면 임시 메시지를 제거한다.

### 3.6 프론트엔드 테스트

단위 테스트:

- `/bubli hello` 입력 시 agent command API 호출
- `/BUBLI hello` 대소문자 무시
- `/bublitest hello`는 일반 메시지 호출
- `/bubli`만 입력하면 API 호출하지 않음
- `요약`, `제안`, `TODO` 키워드에 따라 mode 매핑 확인

통합/E2E 테스트:

- 채팅창에서 `/bubli 가장 최근에 올라온 계약서 알려줘` 입력
- 일반 메시지 API가 호출되지 않는지 확인
- agent command API가 호출되는지 확인
- 응답 후 채팅 목록에 `AGENT_RESPONSE` 메시지가 표시되는지 확인

## 4. 백엔드 개발 계획

### 4.1 1차 구현 범위

프론트에서 기존 API를 호출하는 방식이면 백엔드 신규 API는 필요 없다.

확인할 기존 endpoint:

```http
POST /api/project-rooms/{roomId}/agent/commands
```

백엔드 1차 작업:

1. 해당 API가 프로젝트룸 멤버 권한을 검증하는지 확인한다.
2. 에이전트 응답이 채팅 메시지로 저장되는지 확인한다.
3. 응답 메시지가 웹소켓으로 발행되는지 확인한다.
4. `Accept-Language` 또는 사용자 locale 기준으로 답변 언어가 적용되는지 확인한다.

### 4.2 "가장 최근 계약서" 질의 정확도 보강

현재 agent command API는 존재하지만, "가장 최근에 올라온 계약서"를 정확히 찾으려면 resource 조회 기능이 필요하다.

권장 백엔드 추가 기능:

```java
Optional<ResourceResult> findLatestRoomContract(UUID userId, UUID roomId);
```

위치는 resource 도메인의 public service가 적절하다.

예상 조건:

- 사용자가 해당 프로젝트룸의 active member인지 확인
- `resources.room_id = roomId`
- `visibility = ROOM_SHARED`
- `deleted_at is null`
- `status in (READY, ANALYZED)`
- title 또는 metadata에 계약서 관련 키워드 포함
  - `계약서`
  - `계약`
  - `contract`
  - `agreement`
- `created_at desc` 또는 업로드 완료 시각 desc 기준 1건

### 4.3 Repository 쿼리 예시

`ResourceRepository`에 직접 노출하지 않고 resource service 내부에서 사용한다.

```java
Optional<Resource> findFirstByRoomIdAndVisibilityAndDeletedAtIsNullAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
        UUID roomId,
        ResourceVisibility visibility,
        String title
);
```

단일 keyword만으로는 부족하므로 실제 구현은 `@Query`로 여러 keyword를 처리하는 편이 낫다.

```java
@Query("""
        select r
        from Resource r
        where r.roomId = :roomId
          and r.visibility = :visibility
          and r.deletedAt is null
          and r.status in :statuses
          and (
            lower(r.title) like lower(concat('%', :keyword1, '%'))
            or lower(r.title) like lower(concat('%', :keyword2, '%'))
            or lower(r.title) like lower(concat('%', :keyword3, '%'))
          )
        order by r.createdAt desc
        """)
List<Resource> findLatestContractCandidates(..., Pageable pageable);
```

### 4.4 Agent command service 보강

`ProjectRoomAgentCommandService`에서 사용자 메시지를 분석해 "최근 계약서" 의도가 있으면 resource public service를 호출한다.

처리 흐름:

```text
ProjectRoomAgentCommandService.execute()
  1. 프로젝트룸 멤버 권한 확인
  2. 사용자 locale 확인
  3. message intent 확인
  4. "최근 계약서" 질의이면 ResourcePublicService.findLatestRoomContract(...) 호출
  5. 찾은 resource 정보를 prompt/context에 포함
  6. LLM 또는 fallback 답변 생성
  7. AGENT_RESPONSE 채팅 메시지 저장
```

fallback 답변 예시:

계약서를 찾은 경우:

```text
가장 최근에 올라온 계약서는 "NDA_최종본.pdf"입니다. 업로드 시각은 2026-07-01 10:30입니다.
```

계약서를 찾지 못한 경우:

```text
현재 프로젝트룸에서 계약서로 판단되는 자료를 찾지 못했습니다.
```

### 4.5 응답 body 구조

현재 `AGENT_RESPONSE` body는 `text`, `request`, `mode`, `promptVersion`, `contextCharacters`, `suggestionIds`를 포함한다.

계약서 탐색 결과를 UI에서 활용하려면 optional field를 추가할 수 있다.

```json
{
  "text": "가장 최근에 올라온 계약서는 \"NDA_최종본.pdf\"입니다.",
  "request": "가장 최근에 올라온 계약서 알려줘",
  "mode": "ANSWER",
  "promptVersion": "project-room-agent-command-v1",
  "contextCharacters": 1234,
  "suggestionIds": [],
  "resources": [
    {
      "resourceId": "uuid",
      "title": "NDA_최종본.pdf"
    }
  ]
}
```

초기에는 `text`만으로 충분하다. 리소스 클릭 이동이 필요하면 `resources`를 추가한다.

### 4.6 백엔드 테스트

서비스 테스트:

- `/bubli` prefix는 프론트 책임이므로 백엔드 service test에서는 prefix 없는 message를 기준으로 테스트한다.
- "가장 최근 계약서" 메시지에서 최신 contract resource를 찾아 답변에 포함하는지 확인
- 계약서가 없을 때 fallback 답변 확인
- 프로젝트룸 멤버가 아니면 `PROJECT_403_001` 또는 기존 권한 에러 발생 확인
- 사용자 locale이 `en-US`, `ja-JP`일 때 fallback 답변 언어 확인

통합 테스트:

- `POST /api/project-rooms/{roomId}/agent/commands`
- 요청 body:

```json
{
  "message": "가장 최근에 올라온 계약서 알려줘",
  "mode": "ANSWER",
  "resourceIds": []
}
```

검증:

- HTTP 200
- `data.message.messageType = AGENT_RESPONSE`
- `data.message.body.text`에 최신 계약서 제목 포함
- 채팅 메시지 테이블에 agent response 저장
- room member가 아닌 경우 403

## 5. 단계별 작업 순서

### Phase 1: 프론트 명령어 라우팅

1. 채팅 전송 handler에서 `/bubli` 감지
2. 일반 메시지 API와 agent command API 분기
3. mode 매핑 함수 추가
4. 전송 중/실패 UI 처리
5. 프론트 단위 테스트 작성

완료 기준:

- `/bubli ...` 입력 시 일반 채팅 메시지 API가 호출되지 않는다.
- agent command API가 호출된다.
- 에이전트 응답이 채팅창에 표시된다.

### Phase 2: 백엔드 정확도 보강

1. resource public service에 최신 계약서 조회 기능 추가
2. `ProjectRoomAgentCommandService`에서 최근 계약서 intent 처리
3. fallback 답변에 resource title 포함
4. 서비스/통합 테스트 추가

완료 기준:

- "가장 최근에 올라온 계약서 알려줘" 요청에 실제 최신 계약서 제목으로 답한다.
- 계약서가 없을 때 명확한 fallback 답변을 반환한다.

### Phase 3: UX 개선

1. `/bubli` 명령어 자동완성 또는 helper 표시
2. agent 응답 로딩 placeholder 표시
3. 응답에 포함된 resource를 클릭 가능한 링크로 표시
4. 실패 시 재시도 버튼 제공

완료 기준:

- 사용자가 명령어 사용법을 추측하지 않아도 된다.
- 답변에서 관련 계약서 자료로 바로 이동할 수 있다.

## 6. 주요 결정 사항

- `/bubli` prefix 감지는 프론트엔드 책임으로 둔다.
- 백엔드는 prefix를 모르는 상태로 순수한 사용자 요청만 받는다.
- 일반 채팅 API에는 agent 호출 로직을 넣지 않는다.
- agent command API는 프로젝트룸 전용 기능으로 유지한다.
- "최근 계약서" 같은 도메인 지식은 백엔드 resource public service로 보강한다.

## 7. 리스크

- 프론트에서 `/bubli` 명령을 일반 메시지로도 보내면 중복 메시지가 생긴다.
- 웹소켓 발행이 agent response 저장 흐름에 빠져 있으면 화면 갱신이 늦을 수 있다.
- title keyword만으로 계약서를 찾으면 누락/오탐이 생길 수 있다.
- LLM이 없는 local mode에서는 deterministic fallback 품질이 제한된다.

## 8. Postman 확인 예시

```http
POST http://localhost:8080/api/project-rooms/{roomId}/agent/commands
Authorization: Bearer {accessToken}
Content-Type: application/json
Accept-Language: ko-KR

{
  "message": "가장 최근에 올라온 계약서 알려줘",
  "mode": "ANSWER",
  "resourceIds": []
}
```

예상 응답:

```json
{
  "success": true,
  "data": {
    "message": {
      "messageType": "AGENT_RESPONSE",
      "body": {
        "text": "가장 최근에 올라온 계약서는 \"NDA_최종본.pdf\"입니다."
      }
    },
    "suggestions": []
  },
  "error": null
}
```
