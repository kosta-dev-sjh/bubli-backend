/**
 * [personal] 개인 업무 도메인 그룹.
 * 사용자 개인에게 귀속되는 기능을 하위 패키지로 묶는다.
 *
 * dashboard    - 대시보드: 내 TODO, 담당자 프로젝트룸 TODO, 오늘 일정,
 *               확인 필요 항목, 진행 중 타이머, 새 자료/알림 요약을 조합
 *               → 원본 데이터를 소유하지 않음 (controller/service/dto만)
 * memo         - 개인 메모 CRUD, 프로젝트룸 연결 메모
 *               → 엔티티: Memo (author_user_id, room_id, body, status)
 * timer        - 개인/프로젝트룸 타이머 시작, 일시정지, 종료, 작업 시간 기록
 *               → 엔티티: TimeLog (user_id, room_id, timer_type, started_at, ended_at)
 * notification - 알림 저장/조회 (새메시지, 새버전, 댓글, 에이전트완료, 동기화충돌, 용량초과)
 *               → 엔티티: Notification (user_id, source_type, status: UNREAD/READ/ARCHIVED)
 * calendar     - 구글 캘린더 읽기 연동, 외부 일정 표시
 *               → 외부 연동 중심 (controller/service/dto/type만)
 * suggestion   - 개인 에이전트 제안 표시, 확인 상태 관리
 *               → 표시용 기능 (controller/service/dto/type만)
 *
 * personal은 하위 도메인을 묶는 상위 패키지다.
 * personal 바로 아래에 controller/service/repository/entity/type을 만들지 않는다.
 * 개인 TODO와 프로젝트룸 TODO가 대시보드에 표시되더라도 TODO 원본은 work.task가 소유한다.
 */
package com.bubli.personal;
