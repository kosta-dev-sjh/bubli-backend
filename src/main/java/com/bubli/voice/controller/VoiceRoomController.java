package com.bubli.voice.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.voice.dto.CreateVoiceRoomRequest;
import com.bubli.voice.dto.UpdateMicStatusRequest;
import com.bubli.voice.dto.VoiceParticipantResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
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
        return ApiResponse.success(voiceRoomService.createVoiceRoom(authUser.userId(), request.roomId(), request.chatRoomId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<VoiceRoomResponse> getVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.getVoiceRoom(authUser.userId(), id));
    }

    @GetMapping(params = "roomId")
    public ApiResponse<VoiceRoomResponse> getOpenVoiceRoomByProjectRoom(
            @CurrentUser AuthUser authUser,
            @RequestParam UUID roomId
    ) {
        return ApiResponse.success(voiceRoomService.getOpenVoiceRoomByProjectRoom(authUser.userId(), roomId));
    }

    @GetMapping(params = "chatRoomId")
    public ApiResponse<VoiceRoomResponse> getOpenVoiceRoomByChatRoom(
            @CurrentUser AuthUser authUser,
            @RequestParam UUID chatRoomId
    ) {
        return ApiResponse.success(voiceRoomService.getOpenVoiceRoomByChatRoom(authUser.userId(), chatRoomId));
    }

    @PostMapping("/{id}/token")
    public ApiResponse<VoiceTokenResponse> issueToken(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.issueToken(authUser.userId(), id));
    }

    @PatchMapping("/{id}/mic")
    public ApiResponse<VoiceParticipantResponse> updateMicStatus(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id,
            @RequestBody UpdateMicStatusRequest request
    ) {
        return ApiResponse.success(voiceRoomService.updateMicStatus(authUser.userId(), id, request.micStatus()));
    }

    @PatchMapping("/{id}/leave")
    public ApiResponse<VoiceRoomResponse> leaveVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.leaveVoiceRoom(authUser.userId(), id));
    }

    @PatchMapping("/{id}/decline")
    public ApiResponse<Void> declineVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        voiceRoomService.declineVoiceRoom(authUser.userId(), id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/end")
    public ApiResponse<VoiceRoomResponse> endVoiceRoom(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID id
    ) {
        return ApiResponse.success(voiceRoomService.endVoiceRoom(authUser.userId(), id));
    }
}
