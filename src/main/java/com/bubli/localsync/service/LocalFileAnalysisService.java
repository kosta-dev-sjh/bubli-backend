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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileAnalysisService {

    private static final String SOURCE = "LOCAL_MANAGED_FOLDER_KEY_SENTENCES";

    private final ResourcePublicService resourcePublicService;
    private final AgentJobPublicService agentJobPublicService;

    @Transactional
    public AgentJobResult requestAnalysis(UUID userId, LocalFileAnalysisCommand command) {
        ResourceResult resource = resourcePublicService.getReadableResource(userId, command.resourceId());
        if (resource.visibility() != ResourceVisibility.PERSONAL) {
            throw new BusinessException(ErrorCode.RESOURCE_403_001);
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
                requestPayload(command, extractedText.id())
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

    private Map<String, Object> requestPayload(LocalFileAnalysisCommand command, UUID extractedTextId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("source", SOURCE);
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
