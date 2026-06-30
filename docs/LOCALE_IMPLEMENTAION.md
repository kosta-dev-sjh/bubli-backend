**구현 계획**

1. Locale 정책 확정 계층 추가
- `SupportedLocale` 또는 `AppLocale` enum 추가: `ko-KR`, `en-US`, `ja-JP`
- 기본값은 `ko-KR`
- locale 정규화 규칙 추가:
    - `ko`, `ko-KR` → `ko-KR`
    - `en`, `en-US` → `en-US`
    - `ja`, `ja-JP` → `ja-JP`
    - null/blank/미지원 값 → `ko-KR`
- `UpdateMeRequest.locale` 저장 전에 정규화
- Google OAuth locale도 저장 시 정규화

2. 요청별 Locale 해석기 추가
- `LocaleResolver` 또는 `UserLocaleResolver` 추가
- 우선순위:
    1. 로그인 사용자 `users.locale`
    2. `Accept-Language`
    3. `ko-KR`
- 비로그인 예외 응답도 처리 가능하도록 request header 기반 fallback 제공
- 필요하면 `CurrentUser`만으로 부족하므로 SecurityContext/user lookup 없이도 동작하는 header resolver부터 구현

3. 에러 메시지 다국어화
- `ErrorCode`는 `code`, `httpStatus`, `messageKey`, `defaultMessage` 구조로 전환
- 리소스 번들 추가:
    - `messages_ko.properties`
    - `messages_en.properties`
    - `messages_ja.properties`
- `ErrorResponse`에 가능하면 `messageKey` 추가
- `GlobalExceptionHandler`에서 현재 locale 기준 message resolve
- validation field error도 locale 기준으로 message resolve
- JWT filter / SecurityConfig에서 직접 쓰는 에러 응답도 같은 resolver 사용

4. AI 출력 언어 정책 반영
- `AgentJobQueueMessage` 또는 request payload/context에 `locale` 포함
- `AiJobCommandService`에서 job 생성 시 사용자 locale을 넣거나, execution 시 사용자 locale 조회
- `LlmAgentJobExecutionPort` 프롬프트 변경:
    - “Write all user-facing content in natural Korean” 제거
    - locale별 지시문 삽입:
        - `ko-KR`: natural Korean
        - `en-US`: natural English
        - `ja-JP`: natural Japanese
    - `sourceText`, evidence, 원문 인용은 원문 언어 유지 명시
- `ProjectRoomAgentCommandService`도 같은 정책 적용
- Local fallback title/description/summary를 locale별로 분기

5. 알림/서버 생성 문구 다국어화
- notification 생성 public service에서 message/title을 직접 받는 구조인지 확인
- 서버가 직접 생성하는 문구는 `MessageSource` 기반으로 변경
- 우선순위:
    - agent job 완료/실패 알림
    - suggestion 생성/승인 이벤트성 문구
    - local fallback 문구
- enum/status 값은 번역하지 않고 API에는 코드값 유지

6. API 응답/문서 정책 정리
- `/api/users/me` 응답에 정규화된 locale이 내려가도록 보장
- API 문서에 지원 locale 명시:
    - `ko-KR`
    - `en-US`
    - `ja-JP`
- `Accept-Language` 예시 추가
- 에러 응답 예시 갱신:
    - `code`
    - `messageKey`
    - `message`
    - `traceId`

7. 테스트 계획
- locale 정규화 단위 테스트
- `UpdateMeRequest`/UserService locale 저장 테스트
- `Accept-Language` 기반 에러 메시지 테스트
- 로그인 사용자 locale 우선순위 테스트
- validation 에러 메시지 다국어 테스트
- AI 프롬프트에 locale별 언어 지시문이 들어가는지 테스트
- local fallback이 `ko-KR`, `en-US`, `ja-JP`로 다르게 생성되는지 테스트
- 기존 전체 테스트 실행:
    - `.\gradlew.bat test --console=plain`
    - `.\gradlew.bat test --tests '*ArchitectureTest' --console=plain`

**추천 구현 순서**
1. Locale enum/정규화부터 추가
2. 에러 메시지 다국어화
3. AI 프롬프트/로컬 fallback 언어 분기
4. 알림/서버 생성 문구 확장
5. 문서와 E2E 예시 정리

