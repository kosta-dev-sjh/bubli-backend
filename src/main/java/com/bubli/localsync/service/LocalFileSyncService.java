package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResponse;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileSyncService {

    private final ResourcePublicService resourcePublicService;
    private final UserPublicService userPublicService;

    @Transactional
    public LocalFileSyncResponse sync(UUID userId, List<LocalFileEvent> events) {
        assertManagedFolderConsent(userId);
        List<LocalFileSyncResult> results = new ArrayList<>();
        for (LocalFileEvent event : events) {
            results.add(processEvent(userId, event));
        }
        return new LocalFileSyncResponse(results);
    }

    private LocalFileSyncResult processEvent(UUID userId, LocalFileEvent event) {
        try {
            return switch (event.eventType().toUpperCase()) {
                case "CREATED" -> {
                    String title = event.fileName() != null ? event.fileName() : "untitled";
                    ResourceResult resource = resourcePublicService.createPersonalResource(userId, title);
                    yield new LocalFileSyncResult("CREATED", event.localEventId(), resource.id(), "SYNCED");
                }
                case "DELETED" -> {
                    if (event.resourceId() == null) {
                        yield new LocalFileSyncResult("DELETED", event.localEventId(), null, "SKIPPED");
                    }
                    resourcePublicService.deletePersonalResource(userId, event.resourceId());
                    yield new LocalFileSyncResult("DELETED", event.localEventId(), event.resourceId(), "SYNCED");
                }
                case "UPDATED" -> {
                    if (event.resourceId() == null) {
                        yield new LocalFileSyncResult("UPDATED", event.localEventId(), null, "SKIPPED");
                    }
                    String title = event.fileName() != null ? event.fileName() : "untitled";
                    ResourceResult resource = resourcePublicService.updatePersonalResource(userId, event.resourceId(), title);
                    yield new LocalFileSyncResult("UPDATED", event.localEventId(), resource.id(), "SYNCED");
                }
                default -> {
                    log.warn("Unknown local file event type: {}", event.eventType());
                    yield new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(), "SKIPPED");
                }
            };
        } catch (Exception e) {
            log.warn("Failed to sync local file event: {} - {}", event.eventType(), e.getMessage());
            return new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(), "FAILED");
        }
    }

    private void assertManagedFolderConsent(UUID userId) {
        if (!userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER)) {
            throw new BusinessException(ErrorCode.LOCALSYNC_403_001);
        }
    }
}
