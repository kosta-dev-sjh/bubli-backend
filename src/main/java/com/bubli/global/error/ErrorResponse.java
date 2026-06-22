package com.bubli.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private final String code;
    private final String message;
    private final String traceId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FieldError> fields;

    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), traceId, null);
    }

    public static ErrorResponse of(ErrorCode errorCode, String traceId, List<FieldError> fields) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), traceId, fields);
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private final String field;
        private final String reason;
    }
}
