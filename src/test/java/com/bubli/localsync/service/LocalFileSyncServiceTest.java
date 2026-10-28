package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFileSyncServiceTest {
    @Mock LocalFileSyncEventProcessor eventProcessor;
    @Mock UserPublicService userPublicService;
    @InjectMocks LocalFileSyncService service;

    @Test void delegatesAConsentedChange() {
        UUID user = UUID.randomUUID();
        var event = new LocalFileEvent("CREATED", "a.txt", 10L, "change", "text/plain", null);
        var result = new LocalFileSyncResult("CREATED", "change", UUID.randomUUID(), "SYNCED");
        when(userPublicService.isPrivacyConsentEnabled(user, ConsentType.MANAGED_FOLDER)).thenReturn(true);
        when(eventProcessor.process(user, event)).thenReturn(result);
        assertEquals(List.of(result), service.sync(user, List.of(event)).results());
    }

    @Test void processorFailureDoesNotCancelTheNextChange() {
        UUID user = UUID.randomUUID();
        var first = new LocalFileEvent("CREATED", "a.txt", 10L, "failed", "text/plain", null);
        var second = new LocalFileEvent("CREATED", "b.txt", 10L, "success", "text/plain", null);
        var result = new LocalFileSyncResult("CREATED", "success", UUID.randomUUID(), "SYNCED");
        when(userPublicService.isPrivacyConsentEnabled(user, ConsentType.MANAGED_FOLDER)).thenReturn(true);
        when(eventProcessor.process(user, first)).thenThrow(new IllegalStateException("rolled back"));
        when(eventProcessor.process(user, second)).thenReturn(result);
        var response = service.sync(user, List.of(first, second));
        assertEquals("FAILED", response.results().getFirst().status());
        assertEquals(result, response.results().get(1));
    }

    @Test void requiresConsentBeforeProcessing() {
        UUID user = UUID.randomUUID();
        assertThrows(BusinessException.class, () -> service.sync(user, List.of()));
        verifyNoInteractions(eventProcessor);
    }

    @Test void exhaustedBatchBudgetDoesNotStartMoreTransactions() {
        UUID user = UUID.randomUUID();
        when(userPublicService.isPrivacyConsentEnabled(user, ConsentType.MANAGED_FOLDER)).thenReturn(true);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "batchBudgetMillis", 0L);
        var event = new LocalFileEvent("CREATED", "a.txt", 10L, "change", "text/plain", null);
        var result = service.sync(user, List.of(event)).results().getFirst();
        assertEquals("SYNC_REQUEST_DEADLINE", result.errorCode());
        assertTrue(result.retryable());
        verifyNoInteractions(eventProcessor);
    }
}
