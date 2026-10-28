package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResponse;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileSyncService {
    private final LocalFileSyncEventProcessor eventProcessor;
    private final UserPublicService userPublicService;
    @org.springframework.beans.factory.annotation.Value("${localsync.batch-budget-ms:5000}")
    private long batchBudgetMillis = 5000;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LocalFileSyncResponse sync(UUID userId, List<LocalFileEvent> events) {
        if (!userPublicService.isPrivacyConsentEnabled(userId, ConsentType.MANAGED_FOLDER)) {
            throw new BusinessException(ErrorCode.LOCALSYNC_403_001);
        }
        validateBatch(events);
        List<LocalFileSyncResult> results = new ArrayList<>();
        long startedAt = System.nanoTime();
        for (LocalFileEvent event : events) {
            if (System.nanoTime() - startedAt >= java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(batchBudgetMillis)) {
                results.add(new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(), "FAILED", "SYNC_REQUEST_DEADLINE", true));
                continue;
            }
            try {
                results.add(eventProcessor.process(userId, event));
            } catch (BusinessException error) {
                var code = error.getErrorCode();
                results.add(new LocalFileSyncResult(event.eventType(), event.localEventId(), event.resourceId(),
                        "FAILED", code.getCode(), code.getHttpStatus().is5xxServerError()));
            } catch (RuntimeException error) {
                // The separate bean has already rolled back, including commit failures.
                log.warn("Failed to sync local file event {}", event.localEventId(), error);
                results.add(new LocalFileSyncResult(
                        event.eventType(), event.localEventId(), event.resourceId(), "FAILED", "SYNC_FAILED", true));
            }
        }
        return new LocalFileSyncResponse(results);
    }

    private void validateBatch(List<LocalFileEvent> events) {
        if (events == null || events.isEmpty() || events.size() > 100) throw new BusinessException(ErrorCode.COMMON_400_002);
        var ids = new HashSet<String>();
        for (var event : events) {
            if (event == null || event.localEventId() == null || event.localEventId().isBlank()
                    || event.localEventId().length() > 120 || event.eventType() == null
                    || event.eventType().isBlank() || event.eventType().length() > 40
                    || !ids.add(event.localEventId())) {
                throw new BusinessException(ErrorCode.COMMON_400_002);
            }
        }
    }
}
