package com.bubli.localsync.service;

import com.bubli.localsync.dto.LocalFileEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class LocalFileEventFingerprint {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LocalFileEventFingerprint() {}

    static String of(LocalFileEvent event) {
        // Fixed field order and explicit nulls make retries independent of JSON key order.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", event.eventType().toUpperCase(Locale.ROOT));
        payload.put("fileName", event.fileName());
        payload.put("fileSizeBytes", event.fileSizeBytes());
        payload.put("mimeType", event.mimeType());
        payload.put("resourceId", event.resourceId());
        payload.put("syncDeviceId", event.syncDeviceId());
        payload.put("localSequence", event.localSequence());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(payload));
            return "v1:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException error) {
            throw new IllegalStateException("Cannot fingerprint a local change", error);
        }
    }
}
