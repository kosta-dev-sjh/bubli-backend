package com.bubli.agent.entity;

import com.bubli.agent.type.AgentRequestStatus;
import com.bubli.agent.type.AgentRequestType;
import com.bubli.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "agent_requests",
        indexes = {
                @Index(name = "idx_agent_requests_room_status", columnList = "project_room_id,status"),
                @Index(name = "idx_agent_requests_user_created", columnList = "request_user_id,created_at"),
                @Index(name = "idx_agent_requests_fingerprint", columnList = "request_fingerprint")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_room_id", nullable = false)
    private UUID projectRoomId;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Column(name = "request_user_id", nullable = false)
    private UUID requestUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private AgentRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentRequestStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requestPayload;

    @Column(name = "request_fingerprint", nullable = false, length = 64)
    private String requestFingerprint;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    private AgentRequest(
            UUID projectRoomId,
            UUID sourceDocumentId,
            UUID requestUserId,
            AgentRequestType requestType,
            Map<String, Object> requestPayload,
            String requestFingerprint,
            int maxRetries
    ) {
        this.projectRoomId = require(projectRoomId, "projectRoomId");
        this.sourceDocumentId = sourceDocumentId;
        this.requestUserId = require(requestUserId, "requestUserId");
        this.requestType = require(requestType, "requestType");
        this.requestPayload = immutableJsonMap(require(requestPayload, "requestPayload"));
        this.requestFingerprint = requireHash(requestFingerprint, "requestFingerprint");
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries는 0 이상이어야 합니다.");
        }
        this.maxRetries = maxRetries;
        this.status = AgentRequestStatus.QUEUED;
    }

    public static AgentRequest queue(
            UUID projectRoomId,
            UUID sourceDocumentId,
            UUID requestUserId,
            AgentRequestType requestType,
            Map<String, Object> requestPayload,
            String requestFingerprint,
            int maxRetries
    ) {
        return new AgentRequest(
                projectRoomId,
                sourceDocumentId,
                requestUserId,
                requestType,
                requestPayload,
                requestFingerprint,
                maxRetries
        );
    }

    public void start() {
        if (status != AgentRequestStatus.QUEUED) {
            throw new IllegalStateException("QUEUED 요청만 시작할 수 있습니다.");
        }
        status = AgentRequestStatus.PROCESSING;
        startedAt = Instant.now();
        errorMessage = null;
    }

    public void complete() {
        if (status != AgentRequestStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 요청만 완료할 수 있습니다.");
        }
        status = AgentRequestStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public boolean failOrQueueRetry(String message) {
        if (status != AgentRequestStatus.PROCESSING) {
            throw new IllegalStateException("PROCESSING 요청만 실패 처리할 수 있습니다.");
        }
        errorMessage = requireText(message, "errorMessage");
        retryCount++;

        if (retryCount <= maxRetries) {
            status = AgentRequestStatus.QUEUED;
            startedAt = null;
            return true;
        }

        status = AgentRequestStatus.FAILED;
        completedAt = Instant.now();
        return false;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "는 필수입니다.");
        }
        return value;
    }

    private static String requireHash(String value, String field) {
        String hash = requireText(value, field);
        if (hash.length() != 64) {
            throw new IllegalArgumentException(field + "는 SHA-256 64자리 문자열이어야 합니다.");
        }
        return hash;
    }

    private static Map<String, Object> immutableJsonMap(Map<String, Object> value) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }
}
