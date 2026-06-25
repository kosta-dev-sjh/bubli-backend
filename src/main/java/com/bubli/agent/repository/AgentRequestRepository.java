package com.bubli.agent.repository;

import com.bubli.agent.entity.AgentRequest;
import com.bubli.agent.type.AgentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRequestRepository extends JpaRepository<AgentRequest, UUID> {

    Optional<AgentRequest> findFirstByRequestFingerprintAndStatusInOrderByCreatedAtDesc(
            String requestFingerprint,
            List<AgentRequestStatus> statuses
    );

    List<AgentRequest> findAllByProjectRoomIdAndStatusOrderByCreatedAtDesc(
            UUID projectRoomId,
            AgentRequestStatus status
    );
}
