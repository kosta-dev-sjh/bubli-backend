package com.bubli.personal.memo.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.global.response.PageResponse;
import com.bubli.personal.memo.dto.CreateMemoCommand;
import com.bubli.personal.memo.dto.MemoResult;
import com.bubli.personal.memo.dto.UpdateMemoCommand;
import com.bubli.personal.memo.entity.Memo;
import com.bubli.personal.memo.repository.MemoRepository;
import com.bubli.personal.memo.type.MemoStatus;
import com.bubli.project.service.ProjectMembershipPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoService implements MemoPublicService {

	private final MemoRepository memoRepository;
	private final ProjectMembershipPublicService projectMembershipPublicService;

	@Transactional(readOnly = true)
	public PageResponse<MemoResult> getPersonalMemos(UUID userId, Pageable pageable) {
		return toPage(memoRepository.findByAuthorUserIdAndRoomIdIsNullAndStatus(
				userId,
				MemoStatus.ACTIVE,
				pageable
		));
	}

	@Override
	@Transactional
	public MemoResult createPersonalMemo(UUID userId, CreateMemoCommand command) {
		Memo memo = Memo.createPersonal(userId, command.body());
		return MemoResult.from(memoRepository.save(memo));
	}

	@Transactional(readOnly = true)
	public PageResponse<MemoResult> getRoomMemos(UUID userId, UUID roomId, Pageable pageable) {
		checkRoomMember(userId, roomId);
		return toPage(memoRepository.findByRoomIdAndStatus(roomId, MemoStatus.ACTIVE, pageable));
	}

	@Override
	@Transactional
	public MemoResult createRoomMemo(UUID userId, UUID roomId, CreateMemoCommand command) {
		checkRoomMember(userId, roomId);
		Memo memo = Memo.createRoom(userId, roomId, command.body());
		return MemoResult.from(memoRepository.save(memo));
	}

	@Override
	@Transactional(readOnly = true)
	public List<MemoResult> getUpdatedMemosBetween(UUID userId, Instant from, Instant to, int limit) {
		return memoRepository
				.findByAuthorUserIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
						userId,
						MemoStatus.ACTIVE,
						from,
						to,
						Pageable.ofSize(limit)
				)
				.stream()
				.map(MemoResult::from)
				.toList();
	}

	@Transactional
	public MemoResult updateMemo(UUID userId, UUID memoId, UpdateMemoCommand command) {
		Memo memo = getActiveMemo(memoId);
		checkMemoAccess(userId, memo);
		memo.update(command.body());
		return MemoResult.from(memo);
	}

	@Transactional
	public void deleteMemo(UUID userId, UUID memoId) {
		Memo memo = getActiveMemo(memoId);
		checkMemoAccess(userId, memo);
		memo.delete();
	}

	private Memo getActiveMemo(UUID memoId) {
		Memo memo = memoRepository.findById(memoId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PERSONAL_404_002));
		if (memo.getStatus() == MemoStatus.DELETED) {
			throw new BusinessException(ErrorCode.PERSONAL_404_002);
		}
		return memo;
	}

	private void checkMemoAccess(UUID userId, Memo memo) {
		if (memo.getRoomId() != null) {
			checkRoomMember(userId, memo.getRoomId());
			return;
		}
		if (!userId.equals(memo.getAuthorUserId())) {
			throw new BusinessException(ErrorCode.PERSONAL_403_001);
		}
	}

	private void checkRoomMember(UUID userId, UUID roomId) {
		projectMembershipPublicService.assertActiveMember(userId, roomId);
	}

	private PageResponse<MemoResult> toPage(Page<Memo> page) {
		return new PageResponse<>(
				page.getContent().stream()
						.map(MemoResult::from)
						.toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages(),
				page.hasNext()
		);
	}
}
