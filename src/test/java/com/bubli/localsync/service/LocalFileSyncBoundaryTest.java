package com.bubli.localsync.service;

import com.bubli.localsync.controller.LocalFileSyncAdmission;
import com.bubli.localsync.controller.LocalFileSyncBodyFilter;
import com.bubli.localsync.dto.LocalFileSyncRequest;
import com.bubli.localsync.dto.LocalFileEvent;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileSyncBoundaryTest {
    @Test void controllerKeepsAdmissionStatusHeaderAndErrorCode() {
        var admission = new LocalFileSyncAdmission(1, 1);
        UUID user = UUID.randomUUID();
        var exception = admission.execute(user, () -> assertThrows(LocalFileSyncAdmission.LimitExceeded.class,
                () -> admission.execute(user, () -> true)));
        var factory = org.mockito.Mockito.mock(com.bubli.global.error.ErrorResponseFactory.class);
        var request = new MockHttpServletRequest();
        var error = com.bubli.global.error.ErrorResponse.of(com.bubli.global.error.ErrorCode.LOCALSYNC_429_001, null);
        org.mockito.Mockito.when(factory.of(org.mockito.ArgumentMatchers.eq(com.bubli.global.error.ErrorCode.LOCALSYNC_429_001),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(request))).thenReturn(error);
        var controller = new com.bubli.localsync.controller.LocalFileSyncController(
                org.mockito.Mockito.mock(LocalFileSyncService.class), admission, factory);
        var response = controller.handleAdmissionLimit(exception, request);
        assertEquals(429, response.getStatusCode().value());
        assertEquals("1", response.getHeaders().getFirst("Retry-After"));
        assertFalse(response.getBody().isSuccess());
        assertEquals("LOCALSYNC_429_001", response.getBody().getError().getCode());
    }

    @Test void globalAdmissionAndUserEntryCleanupAreBounded() {
        LocalFileSyncAdmission admission = new LocalFileSyncAdmission(2, 2);
        admission.execute(UUID.randomUUID(), () -> admission.execute(UUID.randomUUID(), () -> {
            assertThrows(ResponseStatusException.class, () -> admission.execute(UUID.randomUUID(), () -> true));
            return true;
        }));
        for(int i=0;i<100;i++) assertTrue(admission.execute(UUID.randomUUID(), () -> true));
        var active=(java.util.Map<?,?>)org.springframework.test.util.ReflectionTestUtils.getField(admission,"active");
        assertTrue(active.isEmpty());
    }

    @Test void oversizedKnownAndUnknownLengthBodiesAreRejectedBeforeParsing() throws Exception {
        for (boolean unknown : new boolean[]{false, true}) {
            MockHttpServletRequest request = new MockHttpServletRequest() {
                @Override public long getContentLengthLong() { return unknown ? -1 : super.getContentLengthLong(); }
            };
            request.setMethod("POST");
            request.setServletPath("/api/local-file-events/sync");
            request.setContent(new byte[65]);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean called = new AtomicBoolean();
            new LocalFileSyncBodyFilter(64).doFilter(request, response, (req, res) -> called.set(true));
            assertEquals(413, response.getStatus());
            assertFalse(called.get());
        }
    }

    @Test void acceptedBodyRemainsReadableAndOtherEndpointsAreUnchanged() throws Exception {
        for (String path : new String[]{"/api/local-file-events/sync", "/api/resources"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setServletPath(path);
            request.setContent("{\"events\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            new LocalFileSyncBodyFilter(64).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                    assertEquals("{\"events\":[]}", new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
        }
    }

    @Test void batchLimitRejectsOverLimit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var event = new LocalFileEvent("CREATED", "file", 1L, "change", "text/plain", null);
            assertFalse(factory.getValidator().validate(new LocalFileSyncRequest(Collections.nCopies(101, event))).isEmpty());
        }
    }

    @Test void userAdmissionRejectsWithoutWaitingAndReleasesAfterFailure() throws Exception {
        LocalFileSyncAdmission admission = new LocalFileSyncAdmission(2, 1);
        UUID user = UUID.randomUUID();
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var first = pool.submit(() -> admission.execute(user, () -> {
                started.countDown();
                try { assertTrue(release.await(5, TimeUnit.SECONDS)); } catch (InterruptedException e) { throw new RuntimeException(e); }
                throw new IllegalStateException("controlled failure");
            }));
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS));
                var rejected = assertThrows(ResponseStatusException.class, () -> admission.execute(user, () -> "unexpected"));
                assertEquals(429, rejected.getStatusCode().value());
                assertEquals("1", rejected.getHeaders().getFirst("Retry-After"));
                assertEquals("other", admission.execute(UUID.randomUUID(), () -> "other"));
            } finally { release.countDown(); }
            assertThrows(java.util.concurrent.ExecutionException.class, () -> first.get(5, TimeUnit.SECONDS));
            assertEquals("recovered", admission.execute(user, () -> "recovered"));
        }
    }
}
