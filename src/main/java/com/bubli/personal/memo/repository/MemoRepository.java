package com.bubli.personal.memo.repository;

import com.bubli.personal.memo.entity.Memo;
import com.bubli.personal.memo.type.MemoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MemoRepository extends JpaRepository<Memo, UUID> {

	Page<Memo> findByAuthorUserIdAndRoomIdIsNullAndStatus(
			UUID authorUserId,
			MemoStatus status,
			Pageable pageable
	);

	Page<Memo> findByRoomIdAndStatus(UUID roomId, MemoStatus status, Pageable pageable);

	List<Memo> findByAuthorUserIdAndStatusAndUpdatedAtGreaterThanEqualAndUpdatedAtLessThanOrderByUpdatedAtDesc(
			UUID authorUserId,
			MemoStatus status,
			Instant from,
			Instant to,
			Pageable pageable
	);
}
