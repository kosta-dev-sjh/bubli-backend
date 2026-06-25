package com.bubli.project.repository;

import com.bubli.project.entity.RoomMember;
import com.bubli.project.type.RoomMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

	boolean existsByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, RoomMemberStatus status);

	Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

	Optional<RoomMember> findByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, RoomMemberStatus status);

	List<RoomMember> findByUserIdAndStatus(UUID userId, RoomMemberStatus status);

	Page<RoomMember> findByRoomIdAndStatus(UUID roomId, RoomMemberStatus status, Pageable pageable);
}
