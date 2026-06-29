package com.bubli.user.repository;

import com.bubli.user.entity.FriendRequest;
import com.bubli.user.type.FriendRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    // 내가 보낸 + 받은 요청 목록
    List<FriendRequest> findByRequesterIdAndStatus(UUID requesterId, FriendRequestStatus status);
    List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);

    // 이미 요청 보냈는지 확인
    boolean existsByRequesterIdAndReceiverIdAndStatus(UUID requesterId, UUID receiverId, FriendRequestStatus status);

    Optional<FriendRequest> findByIdAndReceiverId(UUID id, UUID receiverId);

}
