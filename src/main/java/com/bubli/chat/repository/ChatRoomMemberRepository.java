package com.bubli.chat.repository;

import com.bubli.chat.entity.ChatRoomMember;
import com.bubli.chat.type.ChatMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

	boolean existsByChatRoomIdAndUserIdAndStatus(UUID chatRoomId, UUID userId, ChatMemberStatus status);

	Optional<ChatRoomMember> findByChatRoomIdAndUserIdAndStatus(
			UUID chatRoomId,
			UUID userId,
			ChatMemberStatus status
	);

	Optional<ChatRoomMember> findByChatRoomIdAndUserId(UUID chatRoomId, UUID userId);

	List<ChatRoomMember> findByChatRoomIdAndUserIdIn(UUID chatRoomId, Collection<UUID> userIds);

	List<ChatRoomMember> findByChatRoomIdInAndUserIdNot(Collection<UUID> chatRoomIds, UUID userId);

	List<ChatRoomMember> findByChatRoomIdAndUserIdNotAndStatus(UUID chatRoomId, UUID userId, ChatMemberStatus status);
}
