package com.bubli.chat.service;

import com.bubli.chat.dto.ChatMessageContextResult;
import com.bubli.chat.repository.ChatMessageRepository;
import com.bubli.chat.repository.ChatRoomRepository;
import com.bubli.chat.type.ChatType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMessagePublicServiceImpl implements ChatMessagePublicService {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final ChatRoomAccessPublicService chatRoomAccessPublicService;

	@Override
	@Transactional(readOnly = true)
	public List<ChatMessageContextResult> getRecentRoomMessages(UUID userId, UUID roomId, int limit) {
		return chatRoomRepository.findByRoomIdAndChatType(roomId, ChatType.ROOM)
				.map(chatRoom -> {
					chatRoomAccessPublicService.assertActiveMember(userId, chatRoom.getId());
					return chatMessageRepository.findByChatRoomIdOrderByRoomSequenceDesc(
									chatRoom.getId(),
									PageRequest.of(0, Math.max(1, Math.min(limit, 20)))
							).stream()
							.map(ChatMessageContextResult::from)
							.sorted(Comparator.comparing(ChatMessageContextResult::roomSequence))
							.toList();
				})
				.orElseGet(List::of);
	}
}
