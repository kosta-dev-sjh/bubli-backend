package com.bubli.localsync.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

@Component
public class LocalFileSyncAdmission {
    private final Semaphore slots;
    private final int perUser;
    private final ConcurrentHashMap<UUID, Integer> active = new ConcurrentHashMap<>();

    public LocalFileSyncAdmission(@Value("${localsync.max-concurrent:16}") int total,
                                 @Value("${localsync.max-concurrent-per-user:2}") int perUser) {
        if (total < 1 || perUser < 1 || perUser > total) throw new IllegalArgumentException("Invalid sync concurrency limit");
        this.slots = new Semaphore(total);
        this.perUser = perUser;
    }

    public <T> T execute(UUID userId, Supplier<T> action) {
        if (!slots.tryAcquire()) throw rejected();
        boolean admitted = false;
        try {
            boolean[] accepted = {false};
            active.compute(userId, (id, count) -> {
                int current = count == null ? 0 : count;
                if (current >= perUser) return current;
                accepted[0] = true;
                return current + 1;
            });
            admitted = accepted[0];
            if (!admitted) throw rejected();
            return action.get();
        } finally {
            if (admitted) active.computeIfPresent(userId, (id, count) -> count == 1 ? null : count - 1);
            slots.release();
        }
    }

    private ResponseStatusException rejected() {
        // shortcut: Per JVM only; use a shared admission budget before scaling to multiple servers.
        return new LimitExceeded();
    }

    public static final class LimitExceeded extends ResponseStatusException {
        private LimitExceeded() { super(HttpStatus.TOO_MANY_REQUESTS, "Sync concurrency limit reached"); }
        @Override public org.springframework.http.HttpHeaders getHeaders() {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Retry-After", "1");
            return headers;
        }
    }
}
