package com.bubli.agent.entity;

import com.bubli.agent.type.AgentJobStatus;
import com.bubli.agent.type.AgentJobType;
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

import java.time.Instant;
import java.util.UUID;

/**
 * 에이전트 작업.
 *
 * 테이블: agent_jobs
 * 주요 필드: room_id, resource_id, job_type, status, requested_by, created_at
 *
 * job_type: 문서 분석, 계약 검토, 요구사항 추출, WBS 생성, TODO 생성,
 *           확인 질문 생성, 문서 초안 생성, 하루정리 등
 * status: PENDING → RUNNING → COMPLETED / FAILED
 *
 * 에이전트 분석 실패가 전체 서비스 장애로 이어지면 안 된다. (실패 상태 + 재시도 버튼)
 */
@Entity
@Table(
        name = "agent_jobs",
        indexes = {
                @Index(name = "idx_agent_jobs_room_status", columnList = "room_id,status"),
                @Index(name = "idx_agent_jobs_resource", columnList = "resource_id"),
                @Index(name = "idx_agent_jobs_requested_by", columnList = "requested_by_user_id,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requested_by_user_id", nullable = false)
    private UUID requestedByUserId;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 40)
    private AgentJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AgentJobStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    private AgentJob(
            UUID requestedByUserId,
            UUID roomId,
            UUID resourceId,
            AgentJobType jobType
    ) {
        this.requestedByUserId = require(requestedByUserId, "requestedByUserId");
        this.roomId = roomId;
        this.resourceId = resourceId;
        this.jobType = require(jobType, "jobType");
        this.status = AgentJobStatus.PENDING;
    }

    public static AgentJob pending(
            UUID requestedByUserId,
            UUID roomId,
            UUID resourceId,
            AgentJobType jobType
    ) {
        return new AgentJob(requestedByUserId, roomId, resourceId, jobType);
    }

    public static AgentJob create(
            UUID requestedByUserId,
            UUID roomId,
            UUID resourceId,
            AgentJobType jobType
    ) {
        return pending(requestedByUserId, roomId, resourceId, jobType);
    }

    public void start() {
        if (status != AgentJobStatus.PENDING) {
            throw new IllegalStateException("PENDING 작업만 시작할 수 있습니다.");
        }
        status = AgentJobStatus.RUNNING;
        startedAt = Instant.now();
        errorCode = null;
        errorMessage = null;
    }

    public void markRunning() {
        start();
    }

    public void succeed() {
        if (status != AgentJobStatus.RUNNING) {
            throw new IllegalStateException("RUNNING 작업만 성공 처리할 수 있습니다.");
        }
        status = AgentJobStatus.SUCCEEDED;
        finishedAt = Instant.now();
    }

    public void markSucceeded() {
        succeed();
    }

    public void fail(String errorCode, String errorMessage) {
        if (status != AgentJobStatus.RUNNING && status != AgentJobStatus.PENDING) {
            throw new IllegalStateException("대기 또는 실행 중인 작업만 실패 처리할 수 있습니다.");
        }
        status = AgentJobStatus.FAILED;
        this.errorCode = requireText(errorCode, "errorCode");
        this.errorMessage = requireText(errorMessage, "errorMessage");
        finishedAt = Instant.now();
    }

    public void markFailed(String errorCode, String errorMessage) {
        fail(errorCode, errorMessage);
    }

    public void markDispatchFailed(String errorCode, String errorMessage) {
        retryCount++;
        fail(errorCode, errorMessage);
    }

    public void markRetryQueued() {
        if (status != AgentJobStatus.FAILED) {
            throw new IllegalStateException("Only FAILED jobs can be queued for retry.");
        }
        status = AgentJobStatus.PENDING;
        finishedAt = null;
        errorCode = null;
        errorMessage = null;
    }

    public void cancel() {
        if (status == AgentJobStatus.SUCCEEDED || status == AgentJobStatus.FAILED) {
            throw new IllegalStateException("이미 종료된 작업은 취소할 수 없습니다.");
        }
        status = AgentJobStatus.CANCELED;
        finishedAt = Instant.now();
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }
}
