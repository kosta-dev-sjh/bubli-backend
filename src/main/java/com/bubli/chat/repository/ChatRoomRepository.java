package com.bubli.chat.repository;

import com.bubli.chat.entity.ChatRoom;
import com.bubli.chat.type.ChatMemberStatus;
import com.bubli.chat.type.ChatType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
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

	@Query("""
		select chatRoom
		from ChatRoom chatRoom
		where chatRoom.chatType = :chatType
		  and exists (
			select 1
			from ChatRoomMember member
			where member.chatRoomId = chatRoom.id
			  and member.userId = :firstUserId
			  and member.status = :status
		  )
		  and exists (
			select 1
			from ChatRoomMember member
			where member.chatRoomId = chatRoom.id
			  and member.userId = :secondUserId
			  and member.status = :status
		  )
		""")
	Optional<ChatRoom> findDirectRoomBetween(
			@Param("firstUserId") UUID firstUserId,
			@Param("secondUserId") UUID secondUserId,
			@Param("chatType") ChatType chatType,
			@Param("status") ChatMemberStatus status
	);

	Optional<ChatRoom> findByRoomIdAndChatType(UUID roomId, ChatType chatType);
}
