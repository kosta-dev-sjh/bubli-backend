package com.bubli.localsync.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.error.ErrorResponseFactory;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.global.trace.TraceIdHolder;
import com.bubli.localsync.dto.LocalFileSyncRequest;
import com.bubli.localsync.dto.LocalFileSyncResponse;
import com.bubli.localsync.service.LocalFileSyncService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/local-file-events")
@RequiredArgsConstructor
public class LocalFileSyncController {

    private final LocalFileSyncService localFileSyncService;
    private final LocalFileSyncAdmission admission;
    private final ErrorResponseFactory errorResponseFactory;

    @ExceptionHandler(LocalFileSyncAdmission.LimitExceeded.class)
    public ResponseEntity<ApiResponse<Void>> handleAdmissionLimit(
            LocalFileSyncAdmission.LimitExceeded exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders())
                .body(ApiResponse.fail(errorResponseFactory.of(ErrorCode.LOCALSYNC_429_001, TraceIdHolder.get(), request)));
    }

    @PostMapping("/sync")
    public ApiResponse<LocalFileSyncResponse> sync(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid LocalFileSyncRequest request
    ) {
        return admission.execute(authUser.userId(), () -> ApiResponse.success(localFileSyncService.sync(authUser.userId(), request.events())));
    }
}
