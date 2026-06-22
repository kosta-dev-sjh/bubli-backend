package com.bubli.global.error;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.trace.TraceIdHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, TraceIdHolder.get());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(new ApiResponse.ErrorDetail(
                        errorResponse.getCode(),
                        errorResponse.getMessage(),
                        errorResponse.getTraceId()
                )));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_400_002, TraceIdHolder.get(), fieldErrors);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(new ApiResponse.ErrorDetail(
                        errorResponse.getCode(),
                        errorResponse.getMessage(),
                        errorResponse.getTraceId()
                )));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_500_001, TraceIdHolder.get());
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(new ApiResponse.ErrorDetail(
                        errorResponse.getCode(),
                        errorResponse.getMessage(),
                        errorResponse.getTraceId()
                )));
    }
}
