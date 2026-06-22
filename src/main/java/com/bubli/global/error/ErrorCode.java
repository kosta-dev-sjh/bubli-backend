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
    COMMON_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_001", "서버 오류가 발생했습니다."),

    // AUTH
    AUTH_401_001(HttpStatus.UNAUTHORIZED, "AUTH_401_001", "인증이 필요합니다."),
    AUTH_401_002(HttpStatus.UNAUTHORIZED, "AUTH_401_002", "토큰이 유효하지 않습니다."),

    // PROJECT
    PROJECT_403_001(HttpStatus.FORBIDDEN, "PROJECT_403_001", "프로젝트룸 접근 권한이 없습니다."),
    PROJECT_404_001(HttpStatus.NOT_FOUND, "PROJECT_404_001", "프로젝트를 찾을 수 없습니다."),

    // RESOURCE
    RESOURCE_404_001(HttpStatus.NOT_FOUND, "RESOURCE_404_001", "자료를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
