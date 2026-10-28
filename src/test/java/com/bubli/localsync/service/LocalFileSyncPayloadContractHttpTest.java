package com.bubli.localsync.service;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.localsync.dto.LocalFileEvent;
import com.bubli.localsync.repository.LocalFileSyncEventRepository;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.entity.UserPrivacyConsent;
import com.bubli.user.repository.UserPrivacyConsentRepository;
import com.bubli.user.repository.UserRepository;
import com.bubli.user.type.ConsentType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.profiles.active=test", "spring.profiles.include=", "sentry.dsn=",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '20000'",
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.access-token-expire-ms=1800000", "jwt.refresh-token-expire-ms=2592000000",
        "storage.type=local", "storage.local.base-path=./build/contract-storage"
})
class LocalFileSyncPayloadContractHttpTest extends PostgresIntegrationTestSupport {
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired UserPrivacyConsentRepository consents;
    @Autowired JwtTokenProvider jwt;
    @Autowired LocalFileSyncEventRepository events;
    @LocalServerPort int port;
    UUID user;
    boolean after;
    String testName;
    final List<Map<String, Object>> observations = Collections.synchronizedList(new ArrayList<>());
    final List<Runnable> cleanup = new ArrayList<>();

    @BeforeEach void seed(TestInfo info) {
        String variant = System.getProperty("contract.variant", "after");
        assertTrue(Set.of("before", "after").contains(variant));
        after = variant.equals("after");
        testName = info.getTestMethod().orElseThrow().getName();
        user = createUser("contract");
    }

    UUID createUser(String name) {
        UUID id = users.save(User.createGoogleUser(name, name, name, null, "ko", "Asia/Seoul")).getId();
        consents.save(UserPrivacyConsent.create(id, ConsentType.MANAGED_FOLDER, true));
        return id;
    }

    @AfterEach void recordEvidence() throws Exception {
        cleanup.forEach(Runnable::run);
        jdbc.execute("DROP TRIGGER IF EXISTS contract_completion ON local_file_sync_events");
        jdbc.execute("DROP FUNCTION IF EXISTS contract_fail()");
        Path dir = Path.of("build/contract-http");
        Files.createDirectories(dir);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("variant", after ? "after" : "before");
        output.put("test", testName);
        output.put("observations", observations);
        output.put("eventRows", jdbc.queryForList("SELECT to_jsonb(e)::text AS row FROM local_file_sync_events e ORDER BY local_event_id"));
        output.put("resourceRows", jdbc.queryForList("SELECT id, owner_id, title, local_sync_device_id, local_sync_sequence FROM resources ORDER BY id"));
        output.put("resourceFiles", jdbc.queryForList("SELECT resource_id, original_name, size_bytes FROM resource_files ORDER BY resource_id"));
        output.put("usageRows", jdbc.queryForList("SELECT used_bytes FROM storage_usage"));
        mapper.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(testName + ".json").toFile(), output);
    }

    ObjectNode event(String id) {
        return mapper.valueToTree(new LocalFileEvent("CREATED", "original.txt", 10L, id, "text/plain", null));
    }

    String batch(ObjectNode... changes) throws Exception {
        return mapper.writeValueAsString(Map.of("events", List.of(changes)));
    }

    HttpResponse<String> send(String body) throws Exception { return send(user, body); }

    HttpResponse<String> send(UUID owner, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/local-file-events/sync"))
                .timeout(Duration.ofSeconds(35)).header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwt.createAccessToken(new AuthUser(owner)))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            observations.add(Map.of("request", body, "userId", owner.toString(),
                    "httpStatus", response.statusCode(), "body", response.body()));
            return response;
        }
    }

    JsonNode results(HttpResponse<String> response) throws Exception {
        assertEquals(200, response.statusCode(), response.body());
        JsonNode body = mapper.readTree(response.body());
        JsonNode found = body.at("/data/results");
        if (found.isMissingNode()) found = body.at("/results");
        assertTrue(found.isArray(), response.body());
        return found;
    }

    void failure(JsonNode result, String code) {
        assertEquals("FAILED", result.path("status").asText(), result.toString());
        assertEquals(code, result.path("errorCode").asText(), result.toString());
        assertTrue(result.has("retryable"));
        assertFalse(result.path("retryable").asBoolean(true));
    }

    long count(String table) {
        assertTrue(Set.of("resources", "local_file_sync_events").contains(table));
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    String row(String id) {
        return jdbc.queryForObject("SELECT to_jsonb(e)::text FROM local_file_sync_events e WHERE user_id=? AND local_event_id=?",
                String.class, user, id);
    }

    @Test void equalPayloadReplaysExactlyOnce() throws Exception {
        var body = batch(event("equal"));
        var first = results(send(body)).get(0);
        var second = results(send(body)).get(0);
        assertEquals(first, second);
        assertEquals("SYNCED", first.path("status").asText());
        assertEquals(1, count("resources"));
        assertEquals(1, count("local_file_sync_events"));
        if (after) {
            assertFalse(first.path("retryable").asBoolean(true));
            assertTrue(mapper.readTree(row("equal")).path("payload_fingerprint").asText().matches("v1:[a-f0-9]{64}"));
        }
    }

    @Test void everyChangedRequestFieldRejectsReuseWithoutChangingTheWinner() throws Exception {
        for (String field : List.of("eventType", "fileName", "fileSizeBytes", "mimeType",
                "resourceId", "syncDeviceId", "localSequence")) {
            String id = "changed-" + field;
            ObjectNode firstEvent = event(id);
            var first = results(send(batch(firstEvent))).get(0);
            String beforeRow = row(id);
            ObjectNode changed = firstEvent.deepCopy();
            switch (field) {
                case "eventType" -> changed.put(field, "DELETED");
                case "fileSizeBytes" -> changed.put(field, 20);
                case "resourceId" -> changed.put(field, UUID.randomUUID().toString());
                case "localSequence" -> changed.put(field, 1);
                default -> changed.put(field, "different");
            }
            var second = results(send(batch(changed))).get(0);
            if (after) {
                failure(second, "EVENT_PAYLOAD_MISMATCH");
                assertEquals(changed.path("eventType"), second.path("eventType"));
            } else assertEquals(first, second);
            assertEquals(beforeRow, row(id));
        }
        assertEquals(7, count("resources"));
    }

    @Test void jsonKeyOrderAndEquivalentEventTypeCaseDoNotChangeTheContent() throws Exception {
        ObjectNode original = event("canonical");
        var first = results(send(batch(original))).get(0);
        ObjectNode reordered = mapper.createObjectNode();
        List<String> keys = new ArrayList<>();
        original.fieldNames().forEachRemaining(keys::add);
        Collections.reverse(keys);
        for (String key : keys) reordered.set(key, original.get(key));
        reordered.put("eventType", "created");
        var second = results(send(batch(reordered))).get(0);
        assertEquals(first.path("resourceId"), second.path("resourceId"));
        assertEquals("SYNCED", second.path("status").asText());
        if (after) assertEquals("created", second.path("eventType").asText());
        assertEquals(1, count("resources"));
    }

    @Test void legacyCompletedAndProcessingRowsRemainUnchangedAndExplicitlyUnknown() throws Exception {
        for (String status : List.of("SYNCED", "PROCESSING")) {
            String id = "legacy-" + status;
            jdbc.update("INSERT INTO local_file_sync_events(id,user_id,local_event_id,event_type,status) VALUES (?,?,?,?,?)",
                    UUID.randomUUID(), user, id, "CREATED", status);
            String oldRow = row(id);
            var result = results(send(batch(event(id)))).get(0);
            if (after) failure(result, "EVENT_CONTENT_UNKNOWN");
            else assertEquals(status, result.path("status").asText());
            assertEquals(oldRow, row(id));
        }
        assertEquals(0, count("resources"));
    }

    @Test void validBatchReturnsOneResultForEveryIdIncludingInvalidOrUnboundChanges() throws Exception {
        ObjectNode update = event("missing-update").put("eventType", "UPDATED");
        ObjectNode delete = event("missing-delete").put("eventType", "DELETED");
        ObjectNode unknown = event("unsupported").put("eventType", "RENAMED");
        ObjectNode invalid = event("invalid").put("fileName", " ");
        var response = results(send(batch(event("first"), update, delete, unknown, invalid, event("last"))));
        assertEquals(6, response.size());
        Set<String> ids = new HashSet<>();
        response.forEach(result -> assertTrue(ids.add(result.path("localEventId").asText())));
        assertEquals(Set.of("first", "last", "missing-update", "missing-delete", "unsupported", "invalid"), ids);
        if (after) {
            failure(response.get(1), "RESOURCE_ID_REQUIRED");
            failure(response.get(2), "RESOURCE_ID_REQUIRED");
            failure(response.get(3), "UNSUPPORTED_EVENT_TYPE");
            failure(response.get(4), "INVALID_EVENT_PAYLOAD");
            var replay = results(send(batch(invalid))).get(0);
            assertEquals(response.get(4), replay);
        } else {
            assertEquals("SKIPPED", response.get(1).path("status").asText());
            assertEquals("SKIPPED", response.get(3).path("status").asText());
        }
        assertEquals("SYNCED", response.get(0).path("status").asText());
        assertEquals("SYNCED", response.get(5).path("status").asText());
        assertEquals(2, count("resources"));
    }

    @Test void missingTargetAndOrderingHaveExplicitNonRetryableResults() throws Exception {
        ObjectNode target = event("target").put("eventType", "UPDATED")
                .put("resourceId", UUID.randomUUID().toString()).put("syncDeviceId", "device").put("localSequence", 1);
        ObjectNode order = event("order").put("eventType", "UPDATED").put("resourceId", UUID.randomUUID().toString());
        var response = results(send(batch(target, order)));
        assertEquals("FAILED", response.get(0).path("status").asText());
        assertEquals("FAILED", response.get(1).path("status").asText());
        if (after) {
            failure(response.get(0), "RESOURCE_404_001");
            failure(response.get(1), "ORDER_METADATA_REQUIRED");
        }
        assertEquals(0, count("resources"));
    }

    @Test void duplicateIdsRejectTheEntireBatchBeforeSaving() throws Exception {
        var response = send(batch(event("duplicate"), event("duplicate").put("fileName", "other.txt")));
        if (after) {
            assertEquals(400, response.statusCode());
            assertEquals(0, count("resources"));
            assertEquals(0, count("local_file_sync_events"));
        } else {
            assertEquals(2, results(response).size());
            assertEquals(1, count("resources"));
        }
    }

    @Test void malformedIdsAndJsonRejectTheWholeBatch() throws Exception {
        for (String suffix : List.of("null", "\"\"", "\" \"", "\"" + "x".repeat(121) + "\"", "{}", "[]")) {
            String invalid = "{\"eventType\":\"CREATED\",\"localEventId\":" + suffix + "}";
            var response = send("{\"events\":[" + mapper.writeValueAsString(event("must-not-save")) + "," + invalid + "]}");
            if (after) assertEquals(400, response.statusCode(), response.body());
            observations.add(Map.of("malformedIdBaseline", response.statusCode()));
        }
        assertEquals(0, count("resources"));
        var broken = send("{\"events\":[");
        if (after) assertEquals(400, broken.statusCode());
    }

    @Test void nullElementRejectsTheWholeBatchBeforeSaving() throws Exception {
        var response = send("{\"events\":[" + mapper.writeValueAsString(event("must-not-save")) + ",null]}");
        if (after) {
            assertEquals(400, response.statusCode());
            assertEquals(0, count("resources"));
        } else observations.add(Map.of("baselineNullElementStatus", response.statusCode(), "saved", count("resources")));
    }

    @Test void nonStringIdsAreMalformedRatherThanCoerced() throws Exception {
        for (String id : List.of("42", "true")) {
            var response = send("{\"events\":[{\"eventType\":\"CREATED\",\"localEventId\":" + id + "}]}");
            if (after) assertEquals(400, response.statusCode());
            else observations.add(Map.of("coercedId", id, "httpStatus", response.statusCode()));
        }
        if (after) assertEquals(0, count("resources"));
    }

    @Test void emptyAndMissingEventListsAreMalformed() throws Exception {
        for (String body : List.of("{}", "{\"events\":[]}", "{\"events\":null}")) {
            assertEquals(400, send(body).statusCode());
        }
        assertEquals(0, count("resources"));
    }

    void synchronizeInitialMisses() {
        CountDownLatch bothRead = new CountDownLatch(2);
        AtomicInteger visits = new AtomicInteger();
        MethodInterceptor barrier = invocation -> {
            Object value = invocation.proceed();
            if (invocation.getMethod().getName().equals("findByUserIdAndLocalEventId")
                    && value instanceof Optional<?> result && result.isEmpty() && visits.incrementAndGet() <= 2) {
                bothRead.countDown();
                if (!bothRead.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Second initial miss did not arrive");
            }
            return value;
        };
        var proxy = (Advised) events;
        proxy.addAdvice(0, barrier);
        cleanup.add(() -> proxy.removeAdvice(barrier));
        observations.add(Map.of("synchronization", "Barrier after two initial misses; no DB errors injected"));
    }

    @Test void concurrentEqualRequestsShareOneCommittedResult() throws Exception {
        synchronizeInitialMisses();
        String body = batch(event("concurrent-equal"));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> send(body));
            var second = pool.submit(() -> send(body));
            assertEquals(results(first.get(40, TimeUnit.SECONDS)), results(second.get(40, TimeUnit.SECONDS)));
        }
        assertEquals(1, count("resources"));
        assertEquals(1, count("local_file_sync_events"));
    }

    @Test void concurrentDifferentRequestsChooseOnePayloadAndRejectTheOther() throws Exception {
        synchronizeInitialMisses();
        String firstBody = batch(event("concurrent-different").put("fileName", "first.txt"));
        String secondBody = batch(event("concurrent-different").put("fileName", "second.txt"));
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> send(firstBody));
            var second = pool.submit(() -> send(secondBody));
            JsonNode a = results(first.get(40, TimeUnit.SECONDS)).get(0);
            JsonNode b = results(second.get(40, TimeUnit.SECONDS)).get(0);
            if (after) {
                assertEquals(Set.of("SYNCED", "FAILED"), Set.of(a.path("status").asText(), b.path("status").asText()));
                failure(a.path("status").asText().equals("FAILED") ? a : b, "EVENT_PAYLOAD_MISMATCH");
                String title = jdbc.queryForObject("SELECT title FROM resources", String.class);
                assertEquals(a.path("status").asText().equals("SYNCED") ? "first.txt" : "second.txt", title);
            } else assertEquals(a, b);
        }
        assertEquals(1, count("resources"));
        assertEquals(1, count("local_file_sync_events"));
    }

    @Test void completionFailureRollsBackTheFingerprintAndAllowsARetry() throws Exception {
        jdbc.execute("CREATE FUNCTION contract_fail() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'injected completion failure'; END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER contract_completion BEFORE UPDATE ON local_file_sync_events FOR EACH ROW WHEN (NEW.status = 'SYNCED') EXECUTE FUNCTION contract_fail()");
        var failed = results(send(batch(event("rollback")))).get(0);
        assertEquals("FAILED", failed.path("status").asText());
        assertEquals(0, count("resources"));
        assertEquals(0, count("local_file_sync_events"));
        if (after) {
            assertEquals("SYNC_FAILED", failed.path("errorCode").asText());
            assertTrue(failed.path("retryable").asBoolean());
        }
        jdbc.execute("DROP TRIGGER contract_completion ON local_file_sync_events");
        var retry = results(send(batch(event("rollback").put("fileName", "retried.txt")))).get(0);
        assertEquals("SYNCED", retry.path("status").asText());
        assertEquals("retried.txt", jdbc.queryForObject("SELECT title FROM resources", String.class));
        assertEquals(1, count("resources"));
    }

    @Test void usersDoNotShareTheChangeIdNamespace() throws Exception {
        UUID other = createUser("other");
        var first = results(send(batch(event("user-scoped")))).get(0);
        var second = results(send(other, batch(event("user-scoped").put("fileName", "other.txt")))).get(0);
        assertEquals("SYNCED", second.path("status").asText());
        assertNotEquals(first.path("resourceId"), second.path("resourceId"));
        assertEquals(2, count("resources"));
    }
}
