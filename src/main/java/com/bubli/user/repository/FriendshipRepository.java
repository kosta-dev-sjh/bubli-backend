package com.bubli.user.repository;

import com.bubli.user.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query(value = """
            insert into friendships (
                id,
                user_id,
                friend_user_id,
                accepted_at,
                created_at
            )
            values (
                :id,
                :userId,
                :friendUserId,
                now(),
                now()
            )
            on conflict (user_id, friend_user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("friendUserId") UUID friendUserId
    );
}
