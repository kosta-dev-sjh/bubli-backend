package com.bubli.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorDetail error) {
        return new ApiResponse<>(false, null, error);
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorDetail {
        private final String code;
        private final String message;
        private final String traceId;
    }
}
