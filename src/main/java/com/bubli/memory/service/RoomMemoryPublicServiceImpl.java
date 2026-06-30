package com.bubli.memory.service;

import com.bubli.memory.dto.RoomMemorySummaryContextResult;
import com.bubli.memory.entity.RoomMemorySummary;
import com.bubli.memory.repository.RoomMemorySummaryRepository;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomMemoryPublicServiceImpl implements RoomMemoryPublicService {

	private final RoomMemorySummaryRepository roomMemorySummaryRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Override
	@Transactional(readOnly = true)
	public List<RoomMemorySummaryContextResult> getRecentRoomMemories(UUID userId, UUID roomId, int limit) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		return roomMemorySummaryRepository.findByRoomIdOrderByToSequenceDesc(
						roomId,
						PageRequest.of(0, Math.max(1, Math.min(limit, 10)))
				).stream()
				.map(RoomMemorySummaryContextResult::from)
				.sorted(Comparator.comparing(RoomMemorySummaryContextResult::toSequence))
				.toList();
	}

	@Override
	@Transactional
	public RoomMemorySummaryContextResult createDraft(
			UUID userId,
			UUID roomId,
			Long fromSequence,
			Long toSequence,
			String summaryJson
	) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
		RoomMemorySummary summary = RoomMemorySummary.createDraft(
				roomId,
				fromSequence,
				toSequence,
				summaryJson,
				userId
		);
		return RoomMemorySummaryContextResult.from(roomMemorySummaryRepository.save(summary));
	}
}
