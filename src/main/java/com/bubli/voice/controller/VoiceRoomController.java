package com.bubli.voice.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.voice.dto.CreateVoiceRoomRequest;
import com.bubli.voice.dto.VoiceRoomResponse;
import com.bubli.voice.dto.VoiceTokenResponse;
import com.bubli.voice.service.VoiceRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/voice/rooms")
@RequiredArgsConstructor
public class VoiceRoomController {

    private final VoiceRoomService voiceRoomService;

    @PostMapping
    public ApiResponse<VoiceRoomResponse> createVoiceRoom(
            @CurrentUser AuthUser authUser,
            @RequestBody CreateVoiceRoomRequest request
    ) {
        return ApiResponse.success(voiceRoomService.createVoiceRoom(authUser.userId(), request.roomId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<VoiceRoomResponse> getVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.getVoiceRoom(authUser.userId(), id));
    }

    @PostMapping("/{id}/token")
    public ApiResponse<VoiceTokenResponse> issueToken(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.issueToken(authUser.userId(), id));
    }

    @PatchMapping("/{id}/leave")
    public ApiResponse<VoiceRoomResponse> leaveVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.leaveVoiceRoom(authUser.userId(), id));
    }

    @PatchMapping("/{id}/end")
    public ApiResponse<VoiceRoomResponse> endVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.endVoiceRoom(authUser.userId(), id));
    }
}
