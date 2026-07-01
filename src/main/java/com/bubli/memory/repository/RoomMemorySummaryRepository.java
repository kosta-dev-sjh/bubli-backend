package com.bubli.memory.repository;

import com.bubli.memory.entity.RoomMemorySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomMemorySummaryRepository extends JpaRepository<RoomMemorySummary, UUID> {

	Page<RoomMemorySummary> findByRoomIdOrderByToSequenceDesc(UUID roomId, Pageable pageable);
}
