package com.bubli.localsync.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.localsync.dto.LocalFileSyncRequest;
import com.bubli.localsync.dto.LocalFileSyncResponse;
import com.bubli.localsync.service.LocalFileSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/local-file-events")
@RequiredArgsConstructor
public class LocalFileSyncController {

    private final LocalFileSyncService localFileSyncService;

    @PostMapping("/sync")
    public ApiResponse<LocalFileSyncResponse> sync(
            @CurrentUser AuthUser authUser,
            @RequestBody @Valid LocalFileSyncRequest request
    ) {
        return ApiResponse.success(localFileSyncService.sync(authUser.userId(), request.events()));
    }
}
