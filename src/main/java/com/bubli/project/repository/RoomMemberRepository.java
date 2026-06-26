package com.bubli.project.repository;

import com.bubli.project.entity.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, String> {

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
}
