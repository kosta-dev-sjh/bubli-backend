package com.bubli.chat.repository;

import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.type.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

	boolean existsByChatRoomIdAndUserIdAndStatus(UUID chatRoomId, UUID userId, ChatMemberStatus status);

	Optional<ChatRoomMember> findByChatRoomIdAndUserIdAndStatus(
			UUID chatRoomId,
			UUID userId,
			ChatMemberStatus status
	);
}
