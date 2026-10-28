package com.bubli.resource.service;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.repository.ResourceRepository;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.profiles.active=test", "spring.profiles.include=", "sentry.dsn=",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
        "spring.datasource.hikari.connection-init-sql=SET statement_timeout = '10000'",
        "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
        "jwt.access-token-expire-ms=1800000", "jwt.refresh-token-expire-ms=2592000000",
        "storage.type=local", "storage.local.base-path=./build/write-test-storage"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class ResourceWriteConcurrencyTest extends PostgresIntegrationTestSupport {
    @Autowired ResourceService service;
    @Autowired ResourcePublicService publicService;
    @Autowired ResourceRepository resources;
    @Autowired UserRepository users;
    @Autowired EntityManagerFactory factory;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.bubli.project.repository.ProjectRoomRepository rooms;
    @Autowired com.bubli.project.repository.RoomMemberRepository members;

    UUID owner() { String key = UUID.randomUUID().toString(); return users.save(User.createGoogleUser(key, key, "owner", null, "ko", "Asia/Seoul")).getId(); }

    @Test void concurrentDeleteDeductsUsageOnlyOnce() throws Exception {
        UUID owner = owner();
        UUID deleted = service.createPersonalLocalFileResource(owner, "deleted.txt", 10L, "text/plain").id();
        service.createPersonalLocalFileResource(owner, "retained.txt", 10L, "text/plain");
        boolean baseline = "before".equals(System.getProperty("queue.variant"));
        CountDownLatch read = new CountDownLatch(2), start = new CountDownLatch(1);
        MethodInterceptor barrier = invocation -> {
            Object value = invocation.proceed();
            if (invocation.getMethod().getName().equals("findByIdAndDeletedAtIsNull") && deleted.equals(invocation.getArguments()[0])) {
                read.countDown();
                if (!read.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Baseline reads did not overlap");
            }
            return value;
        };
        if (baseline) ((Advised) resources).addAdvice(barrier);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); service.deleteResource(owner, deleted); return true; });
            var second = pool.submit(() -> { start.await(); service.deleteResource(owner, deleted); return true; });
            start.countDown();
            assertTrue(first.get(20, TimeUnit.SECONDS));
            assertTrue(second.get(20, TimeUnit.SECONDS));
        } finally { if (baseline) ((Advised) resources).removeAdvice(barrier); }
        Long used = jdbc.queryForObject("SELECT used_bytes FROM storage_usage WHERE user_id = ?", Long.class, owner);
        System.out.println("QUEUE_EVIDENCE concurrent_delete used_bytes=" + used);
        assertEquals(10L, used);
    }

    @Test void foreignPersonalDeleteDoesNotChangeResourceOrUsage() {
        UUID owner = owner(), other = owner();
        UUID id = service.createPersonalLocalFileResource(owner, "private.txt", 10L, "text/plain").id();
        assertThrows(RuntimeException.class, () -> publicService.deletePersonalResource(other, id));
        assertNull(resources.findById(id).orElseThrow().getDeletedAt());
        assertEquals(10L, jdbc.queryForObject("SELECT used_bytes FROM storage_usage WHERE user_id = ?", Long.class, owner));
    }

    @Test void personalSyncCannotDeleteSharedRoomResource() {
        UUID owner = owner();
        UUID room = rooms.save(com.bubli.project.entity.ProjectRoom.create(owner, "room", null, null, null, null, null, null)).getId();
        members.save(com.bubli.project.entity.RoomMember.createLeader(room, owner));
        Resource shared = resources.save(Resource.create(owner, room, "shared", com.bubli.resource.type.ResourceKind.FILE,
                com.bubli.resource.type.ResourceVisibility.ROOM_SHARED, com.bubli.resource.type.ResourceStatus.READY));
        assertThrows(RuntimeException.class, () -> publicService.deletePersonalResource(owner, shared.getId()));
        assertNull(resources.findById(shared.getId()).orElseThrow().getDeletedAt());
        service.deleteResource(owner, shared.getId());
        assertNotNull(resources.findById(shared.getId()).orElseThrow().getDeletedAt());
    }

    @Test void staleEntityCannotOverwriteNewerMetadata() {
        UUID owner = owner();
        UUID id = service.createPersonalLocalFileResource(owner, "initial.txt", 10L, "text/plain").id();
        var stale = factory.createEntityManager();
        stale.getTransaction().begin();
        Resource earlier = stale.find(Resource.class, id);
        service.updatePersonalLocalFileResourceIfNewer(owner, id, "latest.txt", 20L, "text/plain", "device", 2L);
        earlier.startAnalysis();
        try { assertThrows(RuntimeException.class, () -> stale.getTransaction().commit()); }
        finally { if (stale.getTransaction().isActive()) stale.getTransaction().rollback(); stale.close(); }
        Resource current = resources.findById(id).orElseThrow();
        assertEquals("latest.txt", current.getTitle());
        assertEquals(2L, current.getLocalSyncSequence());
    }
}
