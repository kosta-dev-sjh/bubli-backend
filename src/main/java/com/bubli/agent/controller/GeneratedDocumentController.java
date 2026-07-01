package com.bubli.agent.controller;

import com.bubli.agent.dto.GeneratedDocumentResponse;
import com.bubli.agent.dto.GeneratedDocumentExportResult;
import com.bubli.agent.service.GeneratedDocumentService;
import com.bubli.global.response.ApiResponse;
import com.bubli.global.response.PageResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GeneratedDocumentController {

    private final GeneratedDocumentService generatedDocumentService;

    @GetMapping("/api/generated-documents")
    public ApiResponse<PageResponse<GeneratedDocumentResponse>> getMine(
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(generatedDocumentService.getMine(authUser.userId(), pageable));
    }

    @GetMapping("/api/project-rooms/{roomId}/generated-documents")
    public ApiResponse<PageResponse<GeneratedDocumentResponse>> getRoomDocuments(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID roomId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(generatedDocumentService.getRoomDocuments(authUser.userId(), roomId, pageable));
    }

    @GetMapping("/api/generated-documents/{documentId}")
    public ApiResponse<GeneratedDocumentResponse> get(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.success(generatedDocumentService.get(authUser.userId(), documentId));
    }

    @GetMapping("/api/generated-documents/{documentId}/export")
    public ResponseEntity<byte[]> exportMarkdown(
            @CurrentUser AuthUser authUser,
            @PathVariable UUID documentId
    ) {
        GeneratedDocumentExportResult result = generatedDocumentService.exportMarkdown(authUser.userId(), documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, result.contentType())
                .body(result.content());
    }
}
