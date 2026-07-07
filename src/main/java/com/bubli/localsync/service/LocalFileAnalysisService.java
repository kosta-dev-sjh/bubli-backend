package com.bubli.localsync.service;

import com.bubli.agent.dto.AgentJobResult;
import com.bubli.agent.service.AgentJobPublicService;
import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileAnalysisCommand;
import com.bubli.localsync.dto.LocalFileAnalysisKeySentence;
import com.bubli.resource.dto.ResourceExtractedTextResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.dto.StoreResourceExtractedTextCommand;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.resource.type.ResourceVisibility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileAnalysisService {

    private static final String SOURCE = "LOCAL_MANAGED_FOLDER_KEY_SENTENCES";
    private static final String ANALYSIS_VERSION = "v1";

    private final ResourcePublicService resourcePublicService;
    private final AgentJobPublicService agentJobPublicService;

    @Transactional
    public AgentJobResult requestAnalysis(UUID userId, LocalFileAnalysisCommand command) {
        ResourceResult resource = resourcePublicService.getReadableResource(userId, command.resourceId());
        if (resource.visibility() != ResourceVisibility.PERSONAL) {
            throw new BusinessException(ErrorCode.RESOURCE_403_001);
        }

        String idempotencyKey = idempotencyKey(userId, command);
        AgentJobResult existingJob = agentJobPublicService.findAnalyzeResourceJobByIdempotencyKey(userId, idempotencyKey);
        if (existingJob != null) {
            return existingJob;
        }

        ResourceExtractedTextResult extractedText = resourcePublicService.storeExtractedText(
                userId,
                storeCommand(command)
        );
        resourcePublicService.startAnalysis(userId, resource.id());

        return agentJobPublicService.createAnalyzeResourceJobResult(
                userId,
                resource.roomId(),
                resource.id(),
                requestPayload(command, extractedText.id(), idempotencyKey)
        );
    }

    private StoreResourceExtractedTextCommand storeCommand(LocalFileAnalysisCommand command) {
        List<Map<String, Object>> sentencesJson = command.keySentences().stream()
                .map(LocalFileAnalysisKeySentence::toJson)
                .toList();

        return new StoreResourceExtractedTextCommand(
                command.resourceId(),
                command.localFileId(),
                command.checksum(),
                command.extractionMethod(),
                command.sourceCharCount(),
                command.analyzedCharCount(),
                command.combinedText(),
                sentencesJson,
                command.textTruncated()
        );
    }

    private Map<String, Object> requestPayload(LocalFileAnalysisCommand command, UUID extractedTextId, String idempotencyKey) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", SOURCE);
        payload.put("idempotencyKey", idempotencyKey);
        payload.put("analysisVersion", ANALYSIS_VERSION);
        payload.put("extractedTextId", extractedTextId.toString());
        payload.put("localFileId", command.localFileId().trim());
        payload.put("fileName", command.fileName().trim());
        payload.put("mimeType", blankToNull(command.mimeType()));
        payload.put("checksum", blankToNull(command.checksum()));
        payload.put("extractionMethod", command.extractionMethod().trim());
        payload.put("sourceCharCount", command.sourceCharCount());
        payload.put("analyzedCharCount", command.analyzedCharCount());
        payload.put("textTruncated", command.textTruncated());
        return payload;
    }

    private String idempotencyKey(UUID userId, LocalFileAnalysisCommand command) {
        String fileIdentity = hasText(command.checksum())
                ? "checksum:" + command.checksum().trim()
                : "localFileId:" + command.localFileId().trim();
        String rawKey = String.join(
                ":",
                SOURCE,
                ANALYSIS_VERSION,
                userId.toString(),
                command.resourceId().toString(),
                fileIdentity,
                command.extractionMethod().trim()
        );
        return "LOCAL_FILE_ANALYSIS:" + sha256(rawKey);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String blankToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
