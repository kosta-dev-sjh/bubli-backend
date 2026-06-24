package com.bubli.work.task.repository;

import com.bubli.work.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	Page<Task> findByOwnerUserIdAndRoomIdIsNull(UUID ownerUserId, Pageable pageable);

	Page<Task> findByRoomId(UUID roomId, Pageable pageable);

	Page<Task> findByAssigneeUserId(UUID assigneeUserId, Pageable pageable);

	boolean existsByWbsItemId(UUID wbsItemId);
}
