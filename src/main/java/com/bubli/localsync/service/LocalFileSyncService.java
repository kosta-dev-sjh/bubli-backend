package com.bubli.localsync.service;

import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResponse;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
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

    @Transactional
    public LocalFileSyncResponse sync(UUID userId, List<LocalFileEvent> events) {
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
                    yield new LocalFileSyncResult("CREATED", resource.id(), "SYNCED");
                }
                case "DELETED" -> {
                    if (event.resourceId() == null) {
                        yield new LocalFileSyncResult("DELETED", null, "SKIPPED");
                    }
                    resourcePublicService.deletePersonalResource(userId, event.resourceId());
                    yield new LocalFileSyncResult("DELETED", event.resourceId(), "SYNCED");
                }
                case "UPDATED" -> {
                    if (event.resourceId() == null) {
                        yield new LocalFileSyncResult("UPDATED", null, "SKIPPED");
                    }
                    String title = event.fileName() != null ? event.fileName() : "untitled";
                    ResourceResult resource = resourcePublicService.updatePersonalResource(userId, event.resourceId(), title);
                    yield new LocalFileSyncResult("UPDATED", resource.id(), "SYNCED");
                }
                default -> {
                    log.warn("Unknown local file event type: {}", event.eventType());
                    yield new LocalFileSyncResult(event.eventType(), event.resourceId(), "SKIPPED");
                }
            };
        } catch (Exception e) {
            log.warn("Failed to sync local file event: {} - {}", event.eventType(), e.getMessage());
            return new LocalFileSyncResult(event.eventType(), event.resourceId(), "FAILED");
        }
    }
}
