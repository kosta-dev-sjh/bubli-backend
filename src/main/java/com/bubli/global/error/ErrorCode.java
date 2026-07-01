package com.bubli.global.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // COMMON
    COMMON_400_001(HttpStatus.BAD_REQUEST, "COMMON_400_001", "잘못된 요청입니다."),
    COMMON_400_002(HttpStatus.BAD_REQUEST, "COMMON_400_002", "요청 값이 올바르지 않습니다."),
    COMMON_404_001(HttpStatus.NOT_FOUND, "COMMON_404_001", "요청한 API를 찾을 수 없습니다."),
    COMMON_405_001(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405_001", "지원하지 않는 HTTP 메서드입니다."),
    COMMON_415_001(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON_415_001", "지원하지 않는 Content-Type입니다."),
    COMMON_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_001", "서버 오류가 발생했습니다."),

    // AUTH
    AUTH_401_001(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "인증이 필요합니다."),
    AUTH_401_002(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "토큰이 유효하지 않습니다."),
    AUTH_401_003(HttpStatus.UNAUTHORIZED, "AUTH_401_003", "토큰이 만료되었습니다."),
    AUTH_401_004(HttpStatus.UNAUTHORIZED, "AUTH_401_004", "Google OAuth code 검증에 실패했습니다."),
    AUTH_401_005(HttpStatus.UNAUTHORIZED, "AUTH_401_005", "Google 계정 식별값을 확인할 수 없습니다."),
    AUTH_401_006(HttpStatus.UNAUTHORIZED, "AUTH_401_006", "refresh token이 유효하지 않습니다."),
    AUTH_401_007(HttpStatus.UNAUTHORIZED, "AUTH_401_007", "refresh token이 만료되었습니다."),
    AUTH_403_001(HttpStatus.FORBIDDEN, "AUTH_403_001", "접근 권한이 없습니다."),
    AUTH_501_001(HttpStatus.NOT_IMPLEMENTED, "AUTH_501_001", "인증 API 구현이 아직 연결되지 않았습니다."),

    // PROJECT
    PROJECT_403_001(HttpStatus.FORBIDDEN, "PROJECT_403_001", "프로젝트룸 접근 권한이 없습니다."),
    PROJECT_403_002(HttpStatus.FORBIDDEN, "PROJECT_403_002", "프로젝트룸 리더만 처리할 수 있습니다."),
    PROJECT_404_001(HttpStatus.NOT_FOUND, "PROJECT_404_001", "프로젝트룸을 찾을 수 없습니다."),
    PROJECT_404_002(HttpStatus.NOT_FOUND, "PROJECT_404_002", "초대를 찾을 수 없습니다."),
    PROJECT_409_001(HttpStatus.CONFLICT, "PROJECT_409_001", "이미 프로젝트룸에 참여 중인 사용자입니다."),
    PROJECT_409_002(HttpStatus.CONFLICT, "PROJECT_409_002", "이미 처리되었거나 만료된 초대입니다."),
    PROJECT_409_003(HttpStatus.CONFLICT, "PROJECT_409_003", "이미 대기 중인 초대가 있습니다."),
    PROJECT_404_003(HttpStatus.NOT_FOUND, "PROJECT_404_003", "초대 링크를 찾을 수 없습니다."),
    PROJECT_410_001(HttpStatus.GONE, "PROJECT_410_001", "만료된 초대 링크입니다."),

    // WORK
    WORK_403_001(HttpStatus.FORBIDDEN, "WORK_403_001", "작업 접근 권한이 없습니다."),
    WORK_404_001(HttpStatus.NOT_FOUND, "WORK_404_001", "TODO를 찾을 수 없습니다."),
    WORK_404_002(HttpStatus.NOT_FOUND, "WORK_404_002", "WBS 항목을 찾을 수 없습니다."),

    // CHAT
    CHAT_403_001(HttpStatus.FORBIDDEN, "CHAT_403_001", "채팅방 접근 권한이 없습니다."),
    CHAT_404_001(HttpStatus.NOT_FOUND, "CHAT_404_001", "채팅방을 찾을 수 없습니다."),
    CHAT_404_002(HttpStatus.NOT_FOUND, "CHAT_404_002", "채팅 메시지를 찾을 수 없습니다."),

    // PERSONAL
    PERSONAL_400_001(HttpStatus.BAD_REQUEST, "PERSONAL_400_001", "타이머 상태가 올바르지 않습니다."),
    PERSONAL_403_001(HttpStatus.FORBIDDEN, "PERSONAL_403_001", "개인 기능 접근 권한이 없습니다."),
    PERSONAL_404_001(HttpStatus.NOT_FOUND, "PERSONAL_404_001", "타이머 기록을 찾을 수 없습니다."),
    PERSONAL_404_002(HttpStatus.NOT_FOUND, "PERSONAL_404_002", "메모를 찾을 수 없습니다."),
    PERSONAL_409_001(HttpStatus.CONFLICT, "PERSONAL_409_001", "이미 실행 중인 타이머가 있습니다."),

    // USER
    USER_404_001(HttpStatus.NOT_FOUND, "USER_404_001", "사용자를 찾을 수 없습니다."),
    USER_404_002(HttpStatus.NOT_FOUND, "USER_404_002", "친구 요청을 찾을 수 없습니다."),
    USER_400_001(HttpStatus.BAD_REQUEST, "USER_400_001", "본인에게 친구 요청을 보낼 수 없습니다."),
    USER_409_001(HttpStatus.CONFLICT, "USER_409_001", "이미 대기 중인 친구 요청이 있습니다."),
    USER_409_002(HttpStatus.CONFLICT, "USER_409_002", "이미 친구인 사용자입니다."),
    USER_409_003(HttpStatus.CONFLICT, "USER_409_003", "이미 처리된 친구 요청입니다."),

    // RESOURCE
    RESOURCE_400_001(HttpStatus.BAD_REQUEST, "RESOURCE_400_001", "자료 요청 값이 올바르지 않습니다."),
    RESOURCE_403_001(HttpStatus.FORBIDDEN, "RESOURCE_403_001", "자료 접근 권한이 없습니다."),
    RESOURCE_404_001(HttpStatus.NOT_FOUND, "RESOURCE_404_001", "자료를 찾을 수 없습니다."),
    RESOURCE_404_002(HttpStatus.NOT_FOUND, "RESOURCE_404_002", "자료 댓글을 찾을 수 없습니다."),
    RESOURCE_404_003(HttpStatus.NOT_FOUND, "RESOURCE_404_003", "자료 파일을 찾을 수 없습니다."),
    RESOURCE_404_004(HttpStatus.NOT_FOUND, "RESOURCE_404_004", "자료 요약을 찾을 수 없습니다."),
    RESOURCE_501_001(HttpStatus.NOT_IMPLEMENTED, "RESOURCE_501_001", "자료 저장소 URL 발급이 아직 연결되지 않았습니다."),
    RESOURCE_501_002(HttpStatus.NOT_IMPLEMENTED, "RESOURCE_501_002", "자료 저장소 저장/삭제가 아직 연결되지 않았습니다."),

    RESOURCE_409_001(HttpStatus.CONFLICT, "RESOURCE_409_001", "Resource already exists."),
    RESOURCE_413_001(HttpStatus.PAYLOAD_TOO_LARGE, "RESOURCE_413_001", "Resource file is too large."),
    RESOURCE_415_001(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "RESOURCE_415_001", "Unsupported resource file type."),
    RESOURCE_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "RESOURCE_500_001", "Resource processing failed."),

    // STORAGE
    STORAGE_400_001(HttpStatus.BAD_REQUEST, "STORAGE_400_001", "저장 용량 요청 값이 올바르지 않습니다."),
    STORAGE_400_002(HttpStatus.BAD_REQUEST, "STORAGE_400_002", "저장 용량 한도를 초과했습니다."),

    // AGENT
    AGENT_400_001(HttpStatus.BAD_REQUEST, "AGENT_400_001", "에이전트 제안 요청 값이 올바르지 않습니다."),
    AGENT_404_001(HttpStatus.NOT_FOUND, "AGENT_404_001", "에이전트 작업을 찾을 수 없습니다."),
    AGENT_404_002(HttpStatus.NOT_FOUND, "AGENT_404_002", "에이전트 제안을 찾을 수 없습니다."),
    AGENT_404_003(HttpStatus.NOT_FOUND, "AGENT_404_003", "AI 문서를 찾을 수 없습니다."),

    // SCHEDULE
    SCHEDULE_400_001(HttpStatus.BAD_REQUEST, "SCHEDULE_400_001", "일정 시간이 올바르지 않습니다."),
    SCHEDULE_403_001(HttpStatus.FORBIDDEN, "SCHEDULE_403_001", "일정 접근 권한이 없습니다."),
    SCHEDULE_404_001(HttpStatus.NOT_FOUND, "SCHEDULE_404_001", "일정을 찾을 수 없습니다."),

    // CALENDAR
    CALENDAR_400_001(HttpStatus.BAD_REQUEST, "CALENDAR_400_001", "Google Calendar 연동 요청 값이 올바르지 않습니다."),
    CALENDAR_404_001(HttpStatus.NOT_FOUND, "CALENDAR_404_001", "Google Calendar 연동 정보를 찾을 수 없습니다."),
    CALENDAR_502_001(HttpStatus.BAD_GATEWAY, "CALENDAR_502_001", "Google Calendar API 호출에 실패했습니다."),

    // NOTIFICATION
    NOTIFICATION_403_001(HttpStatus.FORBIDDEN, "NOTIFICATION_403_001", "알림 접근 권한이 없습니다."),
    NOTIFICATION_404_001(HttpStatus.NOT_FOUND, "NOTIFICATION_404_001", "알림을 찾을 수 없습니다."),

    // ACTIVITY
    ACTIVITY_400_001(HttpStatus.BAD_REQUEST, "ACTIVITY_400_001", "활동 기록 요청 값이 올바르지 않습니다."),
    ACTIVITY_403_001(HttpStatus.FORBIDDEN, "ACTIVITY_403_001", "활동 감지 동의가 필요합니다."),
    ACTIVITY_404_001(HttpStatus.NOT_FOUND, "ACTIVITY_404_001", "활동 기록을 찾을 수 없습니다."),

    // VOICE
    VOICE_404_001(HttpStatus.NOT_FOUND, "VOICE_404_001", "보이스챗 방을 찾을 수 없습니다."),
    VOICE_403_001(HttpStatus.FORBIDDEN, "VOICE_403_001", "보이스챗 방 접근 권한이 없습니다."),
    VOICE_409_001(HttpStatus.CONFLICT, "VOICE_409_001", "이미 종료된 보이스챗 방입니다."),
    VOICE_409_002(HttpStatus.CONFLICT, "VOICE_409_002", "이미 활성화된 보이스챗 방이 있습니다."),

    WIDGET_400_001(HttpStatus.BAD_REQUEST, "WIDGET_400_001", "유효하지 않은 위젯 값입니다."),
    WIDGET_404_001(HttpStatus.NOT_FOUND, "WIDGET_404_001", "위젯 항목을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
