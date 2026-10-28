package com.bubli.localsync.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.dto.LocalFileSyncResult;
import com.bubli.localsync.entity.LocalFileSyncEvent;
import com.bubli.localsync.repository.LocalFileSyncEventRepository;
import com.bubli.resource.service.ResourcePublicService;
import com.bubli.user.service.UserPublicService;
import com.bubli.user.type.ConsentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LocalFileSyncPayloadContractTest {
    final UUID user = UUID.randomUUID();
    final LocalFileEvent event = new LocalFileEvent("CREATED", "a.txt", 10L, "change", null, null);
    final LocalFileSyncEventRepository repository = mock(LocalFileSyncEventRepository.class);
    final ResourcePublicService resources = mock(ResourcePublicService.class);
    final LocalFileSyncEventProcessor processor = new LocalFileSyncEventProcessor(resources, repository);

    @Test void legacyRowsCannotBeReplayedEvenWhenTheySaySynced() {
        when(repository.findByUserIdAndLocalEventId(user, "change")).thenReturn(Optional.of(
                LocalFileSyncEvent.create(user, "change", "CREATED", UUID.randomUUID(), "SYNCED")));
        var response = processor.process(user, event);
        assertEquals("FAILED", response.status());
        assertEquals("EVENT_CONTENT_UNKNOWN", response.errorCode());
        assertFalse(response.retryable());
        verifyNoInteractions(resources);
        verify(repository, never()).insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString());
    }

    @Test void changedPayloadAfterAnInsertConflictDoesNotReuseSuccess() {
        when(repository.findByUserIdAndLocalEventId(user, "change")).thenReturn(Optional.empty(), Optional.of(
                LocalFileSyncEvent.create(user, "change", "CREATED", UUID.randomUUID(), "SYNCED", LocalFileEventFingerprint.of(event))));
        when(repository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(0);
        var changed = new LocalFileEvent("DELETED", "a.txt", 10L, "change", null, UUID.randomUUID());
        var response = processor.process(user, changed);
        assertEquals("FAILED", response.status());
        assertEquals("DELETED", response.eventType());
        assertEquals("EVENT_PAYLOAD_MISMATCH", response.errorCode());
        assertFalse(response.retryable());
        verifyNoInteractions(resources);
    }

    @Test void fingerprintAndPermanentFailureAreWrittenWithTheReservation() {
        when(repository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(1);
        when(repository.updateResult(any(), anyString(), anyString(), nullable(UUID.class), anyString(), any(), nullable(String.class), anyBoolean())).thenReturn(1);
        var unsupported = new LocalFileEvent("RENAMED", "a.txt", 10L, "change", null, null);
        var response = processor.process(user, unsupported);
        assertEquals("UNSUPPORTED_EVENT_TYPE", response.errorCode());
        verify(repository).insertProcessingIfAbsent(any(), eq(user), eq("change"), eq("RENAMED"), any(), eq(LocalFileEventFingerprint.of(unsupported)));
        verify(repository).updateResult(eq(user), eq("change"), eq("RENAMED"), isNull(), eq("FAILED"), any(), eq("UNSUPPORTED_EVENT_TYPE"), eq(false));
    }

    @Test void invalidMetadataAndDeleteWithoutAReferenceAreNotSilentSkips() {
        when(repository.insertProcessingIfAbsent(any(), any(), anyString(), anyString(), any(), anyString())).thenReturn(1);
        when(repository.updateResult(any(), anyString(), anyString(), nullable(UUID.class), anyString(), any(), nullable(String.class), anyBoolean())).thenReturn(1);
        var invalid = new LocalFileEvent("CREATED", " ", -1L, "change", null, null);
        var deleted = new LocalFileEvent("DELETED", "a.txt", 10L, "change", null, null);
        var oversizedMime = new LocalFileEvent("CREATED", "a.txt", 10L, "change", "x".repeat(101), null);
        assertEquals("INVALID_EVENT_PAYLOAD", processor.process(user, invalid).errorCode());
        assertEquals("RESOURCE_ID_REQUIRED", processor.process(user, deleted).errorCode());
        assertEquals("INVALID_EVENT_PAYLOAD", processor.process(user, oversizedMime).errorCode());
        verifyNoInteractions(resources);
    }

    @Test void malformedAndDuplicateBatchesAreRejectedBeforeAnyProcessing() {
        var publicUsers = mock(UserPublicService.class);
        var processing = mock(LocalFileSyncEventProcessor.class);
        when(publicUsers.isPrivacyConsentEnabled(user, ConsentType.MANAGED_FOLDER)).thenReturn(true);
        var service = new LocalFileSyncService(processing, publicUsers);
        assertThrows(BusinessException.class, () -> service.sync(user, null));
        assertThrows(BusinessException.class, () -> service.sync(user, List.of()));
        assertThrows(BusinessException.class, () -> service.sync(user, List.of(event, event)));
        assertThrows(BusinessException.class, () -> service.sync(user, Arrays.asList(event, null)));
        assertThrows(BusinessException.class, () -> service.sync(user, List.of(
                new LocalFileEvent("CREATED", "a", 0L, " ", null, null))));
        verifyNoInteractions(processing);
    }

    @Test void businessFailuresArePermanentAndUnexpectedFailuresRemainRetryable() {
        var publicUsers = mock(UserPublicService.class);
        var processing = mock(LocalFileSyncEventProcessor.class);
        when(publicUsers.isPrivacyConsentEnabled(user, ConsentType.MANAGED_FOLDER)).thenReturn(true);
        var service = new LocalFileSyncService(processing, publicUsers);
        when(processing.process(user, event)).thenThrow(new BusinessException(ErrorCode.RESOURCE_404_001));
        var missing = service.sync(user, List.of(event)).results().getFirst();
        assertEquals("RESOURCE_404_001", missing.errorCode());
        assertFalse(missing.retryable());
        doThrow(new IllegalStateException("rollback")).when(processing).process(user, event);
        var transientFailure = service.sync(user, List.of(event)).results().getFirst();
        assertEquals("SYNC_FAILED", transientFailure.errorCode());
        assertTrue(transientFailure.retryable());
    }

    @Test void fourArgumentResultConstructorRemainsCompatible() {
        assertFalse(new LocalFileSyncResult("CREATED", "change", null, "SYNCED").retryable());
        assertTrue(new LocalFileSyncResult("CREATED", "change", null, "PROCESSING").retryable());
        assertTrue(new LocalFileSyncResult("CREATED", "change", null, "FAILED").retryable());
    }

    @Test void idsMustBeJsonStringsRatherThanCoercedNumbersOrBooleans() throws Exception {
        var mapper = new ObjectMapper();
        for (String id : List.of("42", "true", "{}", "[]")) {
            assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class, () ->
                    mapper.readValue("{\"eventType\":\"CREATED\",\"localEventId\":" + id + "}", LocalFileEvent.class));
        }
        assertEquals("42", mapper.readValue("{\"eventType\":\"CREATED\",\"localEventId\":\"42\"}", LocalFileEvent.class).localEventId());
    }
}
