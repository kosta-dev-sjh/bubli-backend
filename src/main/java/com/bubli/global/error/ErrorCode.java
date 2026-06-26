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

    // WORK
    WORK_403_001(HttpStatus.FORBIDDEN, "WORK_403_001", "작업 접근 권한이 없습니다."),
    WORK_404_001(HttpStatus.NOT_FOUND, "WORK_404_001", "TODO를 찾을 수 없습니다."),
    WORK_404_002(HttpStatus.NOT_FOUND, "WORK_404_002", "WBS 항목을 찾을 수 없습니다."),

    // CHAT
    CHAT_403_001(HttpStatus.FORBIDDEN, "CHAT_403_001", "채팅방 접근 권한이 없습니다."),
    CHAT_404_001(HttpStatus.NOT_FOUND, "CHAT_404_001", "채팅방을 찾을 수 없습니다."),
    CHAT_404_002(HttpStatus.NOT_FOUND, "CHAT_404_002", "채팅 메시지를 찾을 수 없습니다."),

    // PERSONAL
    PERSONAL_404_001(HttpStatus.NOT_FOUND, "PERSONAL_404_001", "타이머 기록을 찾을 수 없습니다."),

    // USER
    USER_404_001(HttpStatus.NOT_FOUND, "USER_404_001", "사용자를 찾을 수 없습니다."),

    // RESOURCE
    RESOURCE_400_001(HttpStatus.BAD_REQUEST, "RESOURCE_400_001", "자료 요청 값이 올바르지 않습니다."),
    RESOURCE_403_001(HttpStatus.FORBIDDEN, "RESOURCE_403_001", "자료 접근 권한이 없습니다."),
    RESOURCE_404_001(HttpStatus.NOT_FOUND, "RESOURCE_404_001", "자료를 찾을 수 없습니다."),
    RESOURCE_404_002(HttpStatus.NOT_FOUND, "RESOURCE_404_002", "자료 댓글을 찾을 수 없습니다."),
    RESOURCE_404_003(HttpStatus.NOT_FOUND, "RESOURCE_404_003", "자료 파일을 찾을 수 없습니다."),
    RESOURCE_404_004(HttpStatus.NOT_FOUND, "RESOURCE_404_004", "자료 요약을 찾을 수 없습니다."),
    RESOURCE_501_001(HttpStatus.NOT_IMPLEMENTED, "RESOURCE_501_001", "자료 저장소 URL 발급이 아직 연결되지 않았습니다."),
    RESOURCE_501_002(HttpStatus.NOT_IMPLEMENTED, "RESOURCE_501_002", "자료 저장소 저장/삭제가 아직 연결되지 않았습니다."),

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

    // NOTIFICATION
    NOTIFICATION_403_001(HttpStatus.FORBIDDEN, "NOTIFICATION_403_001", "알림 접근 권한이 없습니다."),
    NOTIFICATION_404_001(HttpStatus.NOT_FOUND, "NOTIFICATION_404_001", "알림을 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
