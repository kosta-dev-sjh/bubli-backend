package com.bubli.resource.service;

import com.bubli.global.security.AuthUser;
import com.bubli.global.security.JwtTokenProvider;
import com.bubli.localsync.controller.LocalFileSyncAdmission;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.profiles.active=test", "spring.profiles.include=", "sentry.dsn=",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.access-token-expire-ms=1800000", "jwt.refresh-token-expire-ms=2592000000",
        "storage.type=local", "storage.local.base-path=./build/write-test-storage"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class LocalFileSyncMockBoundaryTest extends PostgresIntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtTokenProvider jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired LocalFileSyncAdmission admission;

    UUID user() { String key=UUID.randomUUID().toString(); return users.save(User.createGoogleUser(key,key,"owner",null,"ko","Asia/Seoul")).getId(); }
    String token(UUID id) { return "Bearer " + jwt.createAccessToken(new AuthUser(id)); }
    void assertNoEvents() { assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM local_file_sync_events", Integer.class)); }

    @Test void oversizedJsonIsRejectedByActualFilterBeforeStorage() throws Exception {
        mvc.perform(post("/api/local-file-events/sync").servletPath("/api/local-file-events/sync")
                .header("Authorization",token(user())).contentType("application/json").content(" ".repeat(262145)))
                .andExpect(status().isPayloadTooLarge());
        assertNoEvents();
    }

    @Test void oversizedBatchIsRejectedByActualValidationBeforeStorage() throws Exception {
        String events=IntStream.range(0,101).mapToObj(i -> "{\"eventType\":\"CREATED\",\"localEventId\":\"change-"+i+"\"}").collect(Collectors.joining(","));
        mvc.perform(post("/api/local-file-events/sync").servletPath("/api/local-file-events/sync")
                .header("Authorization",token(user())).contentType("application/json").content("{\"events\":["+events+"]}"))
                .andExpect(status().isBadRequest());
        assertNoEvents();
    }

    @Test void saturatedUserGets429WithRetryHeaderBeforeStorage() {
        UUID user=user();
        admission.execute(user, () -> admission.execute(user, () -> {
            try {
                mvc.perform(post("/api/local-file-events/sync").servletPath("/api/local-file-events/sync")
                        .header("Authorization",token(user)).contentType("application/json")
                        .content("{\"events\":[{\"eventType\":\"CREATED\",\"localEventId\":\"change\"}]}"))
                        .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","1"))
                        .andExpect(jsonPath("$.error.code").value("LOCALSYNC_429_001"));
            } catch (Exception e) { throw new RuntimeException(e); }
            return true;
        }));
        assertNoEvents();
    }
}
