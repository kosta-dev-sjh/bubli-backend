package com.bubli.resource.controller;

import com.bubli.global.response.ApiResponse;
import com.bubli.global.security.AuthUser;
import com.bubli.global.security.CurrentUser;
import com.bubli.resource.dto.ContractDocumentUploadResponse;
import com.bubli.resource.service.DocumentUploadService;
import com.bubli.resource.type.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentUploadService documentUploadService;

    @PostMapping(
            value = "/api/project-rooms/{roomId}/contract-documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ContractDocumentUploadResponse>> uploadContractDocument(
            @PathVariable UUID roomId,
            @RequestParam DocumentType documentType,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "true") boolean autoAnalyze,
            @CurrentUser AuthUser currentUser
    ) {
        ContractDocumentUploadResponse response = documentUploadService.uploadContractDocument(
                roomId,
                currentUser.userId(),
                documentType,
                file,
                autoAnalyze
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
