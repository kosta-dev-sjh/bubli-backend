package com.bubli.localsync.service;

import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.localsync.entity.LocalFileSyncEvent;
import com.bubli.localsync.repository.LocalFileSyncEventRepository;
import com.bubli.resource.dto.ResourceResult;
import com.bubli.resource.service.ResourcePublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileSyncEventProcessor {
    private final ResourcePublicService resourcePublicService;
    private final LocalFileSyncEventRepository eventRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED, timeout = 10)
    public LocalFileSyncResult process(UUID userId, LocalFileEvent event) {
        if ((event.fileName() != null && (event.fileName().isBlank() || event.fileName().length() > 255))
                || (event.fileSizeBytes() != null && event.fileSizeBytes() < 0)
                || (event.mimeType() != null && event.mimeType().length() > 100)) {
            return failed(event, "INVALID_EVENT_PAYLOAD");
        }
        String fingerprint = LocalFileEventFingerprint.of(event);
        var existing = eventRepository.findByUserIdAndLocalEventId(userId, event.localEventId());
        if (existing.isPresent()) {
            return replay(existing.get(), event, fingerprint);
        }
        int inserted = eventRepository.insertProcessingIfAbsent(
                UUID.randomUUID(), userId, event.localEventId(), event.eventType(), Instant.now(), fingerprint);
        if (inserted == 0) {
            // A new Read Committed statement sees the winner after its commit.
            return eventRepository.findByUserIdAndLocalEventId(userId, event.localEventId())
                    .map(record -> replay(record, event, fingerprint))
                    .orElseThrow(() -> new IllegalStateException("Conflicting sync result is missing"));
        }
        LocalFileSyncResult result = apply(userId, event);
        int updated = eventRepository.updateResult(userId, result.localEventId(), result.eventType(),
                result.resourceId(), result.status(), Instant.now(), result.errorCode(), result.retryable());
        if (updated != 1) {
            throw new IllegalStateException("Expected exactly one processing record to complete");
        }
        return result;
    }

    private LocalFileSyncResult apply(UUID userId, LocalFileEvent event) {
        if ((event.fileName() != null && (event.fileName().isBlank() || event.fileName().length() > 255))
                || (event.fileSizeBytes() != null && event.fileSizeBytes() < 0)
                || (event.mimeType() != null && event.mimeType().length() > 100)) {
            return failed(event, "INVALID_EVENT_PAYLOAD");
        }
        return switch (event.eventType().toUpperCase(java.util.Locale.ROOT)) {
            case "CREATED" -> {
                String title = event.fileName() != null ? event.fileName() : "untitled";
                ResourceResult resource = resourcePublicService.createPersonalLocalFileResource(
                        userId, title, event.fileSizeBytes(), event.mimeType());
                yield new LocalFileSyncResult(event.eventType(), event.localEventId(), resource.id(), "SYNCED");
            }
            case "DELETED" -> {
                if (event.resourceId() == null) {
                    yield failed(event, "RESOURCE_ID_REQUIRED");
                }
                resourcePublicService.deletePersonalResource(userId, event.resourceId());
                yield new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(), "SYNCED");
            }
            case "UPDATED" -> {
                if (event.resourceId() == null) {
                    yield failed(event, "RESOURCE_ID_REQUIRED");
                }
                if (event.syncDeviceId() == null || event.syncDeviceId().isBlank()
                        || event.syncDeviceId().length() > 120 || event.localSequence() == null
                        || event.localSequence() <= 0 || event.localSequence() > 9007199254740991L) {
                    yield failed(event, "ORDER_METADATA_REQUIRED");
                }
                String title = event.fileName() != null ? event.fileName() : "untitled";
                boolean applied = resourcePublicService.updatePersonalLocalFileResourceIfNewer(
                        userId, event.resourceId(), title, event.fileSizeBytes(), event.mimeType(),
                        event.syncDeviceId(), event.localSequence());
                yield new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(),
                        applied ? "SYNCED" : "SUPERSEDED");
            }
            default -> failed(event, "UNSUPPORTED_EVENT_TYPE");
        };
    }

    private LocalFileSyncResult replay(LocalFileSyncEvent record, LocalFileEvent incoming, String fingerprint) {
        if (record.getPayloadFingerprint() == null || record.getPayloadFingerprint().isBlank()) {
            return failed(incoming, "EVENT_CONTENT_UNKNOWN");
        }
        if (!fingerprint.equals(record.getPayloadFingerprint())) {
            return failed(incoming, "EVENT_PAYLOAD_MISMATCH");
        }
        return new LocalFileSyncResult(incoming.eventType(), incoming.localEventId(),
                record.getResourceId(), record.getStatus(), record.getErrorCode(), record.isRetryable());
    }

    private LocalFileSyncResult failed(LocalFileEvent event, String code) {
        return new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(),
                "FAILED", code, false);
    }
}
