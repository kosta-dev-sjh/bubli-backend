package com.bubli.chat.repository;

import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.type.ChatMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

	@Query(value = """
		select chatRoom
		from ChatRoom chatRoom
		where exists (
			select 1
			from ChatRoomMember member
			where member.chatRoomId = chatRoom.id
			  and member.userId = :userId
			  and member.status = :status
		)
		order by chatRoom.updatedAt desc
		""",
		countQuery = """
		select count(chatRoom)
		from ChatRoom chatRoom
		where exists (
			select 1
			from ChatRoomMember member
			where member.chatRoomId = chatRoom.id
			  and member.userId = :userId
			  and member.status = :status
		)
		""")
	Page<ChatRoom> findAccessibleRooms(@Param("userId") UUID userId,
			@Param("status") ChatMemberStatus status,
			Pageable pageable);
}
