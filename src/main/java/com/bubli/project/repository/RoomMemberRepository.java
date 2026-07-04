package com.bubli.project.repository;

import com.bubli.project.entity.RoomMember;
import com.bubli.project.type.RoomMemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    @Query(
            value = """
                    SELECT COUNT(*) > 0
                    FROM room_members
                    WHERE room_id = :roomId
                      AND user_id = :userId
                      AND status = 'ACTIVE'
                    """,
            nativeQuery = true
    )
    boolean existsActiveMember(
            @Param("roomId") UUID roomId,
            @Param("userId") UUID userId
    );

    boolean existsByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, RoomMemberStatus status);

    Optional<RoomMember> findByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, RoomMemberStatus status);

    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    Page<RoomMember> findByRoomIdAndStatus(UUID roomId, RoomMemberStatus status, Pageable pageable);

    List<RoomMember> findByRoomIdAndStatus(UUID roomId, RoomMemberStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select member from RoomMember member where member.roomId = :roomId and member.status = :status")
    List<RoomMember> findByRoomIdAndStatusForUpdate(
            @Param("roomId") UUID roomId,
            @Param("status") RoomMemberStatus status
    );

    List<RoomMember> findByUserIdAndStatus(UUID userId, RoomMemberStatus status);
}
