package com.bubli.localsync.service;

import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.entity.LocalFileSyncEvent;
import com.bubli.localsync.repository.LocalFileSyncEventRepository;
import com.bubli.resource.service.ResourcePublicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalFileSyncEventProcessorTest {
    @Mock ResourcePublicService resourcePublicService;
    @Mock LocalFileSyncEventRepository eventRepository;
    @InjectMocks LocalFileSyncEventProcessor processor;
    final UUID user = UUID.randomUUID(), resource = UUID.randomUUID();
    final LocalFileEvent created = new LocalFileEvent("CREATED", "a.txt", 10L, "change", "text/plain", null);

    @BeforeEach void defaults() {
        lenient().when(eventRepository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(1);
        lenient().when(eventRepository.updateResult(any(), anyString(), anyString(), nullable(UUID.class), anyString(), any(), nullable(String.class), anyBoolean())).thenReturn(1);
    }

    @Test void completedReplayDoesNotCreateAnotherResource() {
        when(eventRepository.findByUserIdAndLocalEventId(user, "change")).thenReturn(Optional.of(
                LocalFileSyncEvent.create(user, "change", "CREATED", resource, "SYNCED", LocalFileEventFingerprint.of(created))));
        assertEquals(resource, processor.process(user, created).resourceId());
        verifyNoInteractions(resourcePublicService);
        verify(eventRepository, never()).insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test void insertConflictReadsTheCommittedWinner() {
        when(eventRepository.findByUserIdAndLocalEventId(user, "change")).thenReturn(Optional.empty(), Optional.of(
                LocalFileSyncEvent.create(user, "change", "CREATED", resource, "SYNCED", LocalFileEventFingerprint.of(created))));
        when(eventRepository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(0);
        assertEquals(resource, processor.process(user, created).resourceId());
        verifyNoInteractions(resourcePublicService);
    }

    @Test void missingConflictResultIsNotManufacturedAsProcessing() {
        when(eventRepository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> processor.process(user, created));
        verifyNoInteractions(resourcePublicService);
    }

    @Test void existingProcessingRecordIsNotDeletedOrRecreated() {
        when(eventRepository.findByUserIdAndLocalEventId(user, "change")).thenReturn(Optional.of(
                LocalFileSyncEvent.create(user, "change", "CREATED", null, "PROCESSING", LocalFileEventFingerprint.of(created))));
        assertEquals("PROCESSING", processor.process(user, created).status());
        verify(eventRepository, never()).deleteProcessingEvent(any(), anyString());
        verifyNoInteractions(resourcePublicService);
    }

    @Test void missingUpdateTargetFailsWithoutCreatingResources() {
        var event = new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", null);
        var result = processor.process(user, event);
        assertEquals("FAILED", result.status());
        assertEquals("RESOURCE_ID_REQUIRED", result.errorCode());
        assertFalse(result.retryable());
        verifyNoInteractions(resourcePublicService);
    }

    @Test void olderUpdateIsTerminalButNotAppliedAgain() {
        var event = new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", resource, "device", 2L);
        when(resourcePublicService.updatePersonalLocalFileResourceIfNewer(user, resource, "a.txt", 10L, "text/plain", "device", 2L)).thenReturn(false);
        assertEquals("SUPERSEDED", processor.process(user, event).status());
    }

    @Test void updateRequiresOrderingMetadata() {
        var event = new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", resource);
        assertEquals("ORDER_METADATA_REQUIRED", processor.process(user, event).errorCode());
        verifyNoInteractions(resourcePublicService);
    }

    @Test void zeroCompletedRecordsRaisesAnError() {
        when(eventRepository.updateResult(any(), anyString(), anyString(), nullable(UUID.class), anyString(), any(), nullable(String.class), anyBoolean())).thenReturn(0);
        var event = new LocalFileEvent("DELETED", "a.txt", 10L, "change", "text/plain", null);
        assertThrows(IllegalStateException.class, () -> processor.process(user, event));
    }
}
