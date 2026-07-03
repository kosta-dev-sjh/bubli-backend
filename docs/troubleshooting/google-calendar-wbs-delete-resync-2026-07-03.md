# Google Calendar WBS 삭제 재동기화 트러블슈팅

날짜: 2026-07-03

## 증상

- WBS 기간을 삭제했는데 Bubli 화면에서 다시 살아나거나, Google Calendar와 반영 시점이 어긋난다.
- Google Calendar에는 이미 없는 일정인데 Bubli DB에만 남은 row가 삭제 흐름에서 계속 걸릴 수 있다.
- Google Calendar 삭제 요청이 실패하면 로컬 삭제까지 실패한 것처럼 보인다.

## 원인

1. Bubli 일정/WBS 삭제와 외부 Google Calendar 삭제가 같은 사용자 행동 안에 묶여 있었다.
2. Google 삭제 실패를 별도로 기억하지 않으면 다음 sync에서 같은 `googleEventId`가 다시 내려올 때 로컬 일정으로 되살릴 수 있었다.
3. Google Calendar에서 이미 사라진 이벤트는 `DELETE`가 404/410을 줄 수 있는데, 이 경우는 실패가 아니라 삭제 완료로 봐야 한다.

## 수정한 흐름

1. 사용자가 WBS/일정을 삭제하면 Bubli DB 삭제는 먼저 확정한다.
2. Google Calendar 삭제가 성공하면 그대로 끝낸다.
3. Google Calendar가 404/410을 주면 이미 없는 이벤트이므로 성공으로 처리한다.
4. Google Calendar 삭제가 실패하면 `google_calendar_delete_requests`에 삭제 대기 항목을 남긴다.
5. 이후 `POST /api/calendar/sync`에서 같은 `googleEventId`가 다시 내려오면 로컬 DB에 upsert하지 않는다.
6. 대신 Google Calendar 삭제를 다시 요청하고, 성공하면 삭제 대기 항목을 지운다.

## 기대 결과

- 사용자가 삭제한 WBS/일정은 화면에서 즉시 사라진다.
- 외부 Google Calendar 반영이 늦거나 일시 실패해도 Bubli DB에 다시 살아나지 않는다.
- Google Calendar에 실제로 남아 있던 이벤트는 다음 sync 때 다시 삭제 요청된다.
- Google Calendar에 이미 없는 이벤트는 성공으로 간주되어 삭제 대기 상태가 정리된다.

## 남은 확인

- 실제 Google 계정으로 WBS 기간 생성 후 Bubli에서 삭제했을 때 Google Calendar가 바로 지워지는지 확인한다.
- 네트워크 실패나 토큰 문제로 Google 삭제가 실패한 뒤 `POST /api/calendar/sync`를 호출하면 삭제 재시도가 되는지 확인한다.
