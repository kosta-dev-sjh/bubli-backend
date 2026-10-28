package com.bubli.localsync.service;

import com.bubli.localsync.dto.LocalFileEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileEventFingerprintTest {
    @Test void fingerprintIsVersionedDeterministicAndCaseNormalizesOnlyTheEventType() {
        var first = new LocalFileEvent("CREATED", "a.txt", 10L, "change", null, null);
        var second = new LocalFileEvent("created", "a.txt", 10L, "other-id", null, null);
        assertEquals(LocalFileEventFingerprint.of(first), LocalFileEventFingerprint.of(second));
        assertTrue(LocalFileEventFingerprint.of(first).matches("v1:[a-f0-9]{64}"));
    }

    @Test void everyPayloadFieldChangesTheFingerprintIncludingNullAndEmpty() {
        UUID resource = UUID.randomUUID();
        var first = new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", resource, "device", 1L);
        for (var changed : List.of(
                new LocalFileEvent("DELETED", "a.txt", 10L, "change", "text/plain", resource, "device", 1L),
                new LocalFileEvent("UPDATED", "a\n\".txt", 10L, "change", "text/plain", resource, "device", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 11L, "change", "text/plain", resource, "device", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "", resource, "device", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 10L, "change", null, resource, "device", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", null, "device", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", resource, "other", 1L),
                new LocalFileEvent("UPDATED", "a.txt", 10L, "change", "text/plain", resource, "device", 2L))) {
            assertNotEquals(LocalFileEventFingerprint.of(first), LocalFileEventFingerprint.of(changed));
        }
    }
}
