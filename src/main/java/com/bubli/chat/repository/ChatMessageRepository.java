package com.bubli.chat.repository;

import com.bubli.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

	Optional<ChatMessage> findByChatRoomIdAndClientMessageId(UUID chatRoomId, String clientMessageId);

	Optional<ChatMessage> findByIdAndChatRoomId(UUID id, UUID chatRoomId);

	@Query("""
		select coalesce(max(message.roomSequence), 0)
		from ChatMessage message
		where message.chatRoomId = :chatRoomId
		""")
	long findMaxRoomSequence(@Param("chatRoomId") UUID chatRoomId);

	Page<ChatMessage> findByChatRoomIdOrderByRoomSequenceDesc(UUID chatRoomId, Pageable pageable);

	Page<ChatMessage> findByChatRoomIdAndRoomSequenceGreaterThanOrderByRoomSequenceAsc(
			UUID chatRoomId,
			Long roomSequence,
			Pageable pageable
	);

	Page<ChatMessage> findByChatRoomIdAndRoomSequenceLessThanOrderByRoomSequenceDesc(
			UUID chatRoomId,
			Long roomSequence,
			Pageable pageable
	);
}
