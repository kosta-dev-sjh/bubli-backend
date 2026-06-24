package com.bubli.project.repository;

import com.bubli.project.entity.ProjectRoomEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRoomEventRepository extends JpaRepository<ProjectRoomEvent, UUID> {

	Page<ProjectRoomEvent> findByRoomIdAndSequenceGreaterThanOrderBySequenceAsc(
			UUID roomId,
			Long sequence,
			Pageable pageable
	);

	Optional<ProjectRoomEvent> findTopByRoomIdOrderBySequenceDesc(UUID roomId);
}
