package com.bubli.global.error;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.trace.TraceIdHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseFactory errorResponseFactory;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse errorResponse = errorResponseFactory.of(errorCode, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = errorResponseFactory.of(
                ErrorCode.COMMON_400_002,
                TraceIdHolder.get(),
                request,
                e.getBindingResult().getFieldErrors()
        );
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HandlerMethodValidationException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(Exception e, HttpServletRequest request) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.COMMON_400_002, TraceIdHolder.get(), request);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.RESOURCE_413_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.AUTH_401_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.AUTH_403_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e, HttpServletRequest request) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.COMMON_404_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.COMMON_405_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.COMMON_415_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.fail(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        ErrorResponse errorResponse = errorResponseFactory.of(ErrorCode.COMMON_500_001, TraceIdHolder.get(), request);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(errorResponse));
    }
}
