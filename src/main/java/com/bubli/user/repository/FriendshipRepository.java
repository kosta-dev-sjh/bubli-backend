package com.bubli.user.repository;

import com.bubli.user.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // 친구 목록 조회
    List<Friendship> findByUserId(UUID userId);

    // 이미 친구인지 확인
    boolean existsByUserIdAndFriendUserId(UUID userId, UUID friendUserId);

    @Transactional
    void deleteByUserIdAndFriendUserId(UUID userId, UUID friendUserId);
}
