package com.bubli.work.task.repository;

import com.bubli.work.task.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

	Page<Task> findByOwnerUserIdAndRoomIdIsNull(UUID ownerUserId, Pageable pageable);

	Page<Task> findByRoomId(UUID roomId, Pageable pageable);

	List<Task> findByRoomIdOrderByUpdatedAtDesc(UUID roomId);

	Page<Task> findByAssigneeUserId(UUID assigneeUserId, Pageable pageable);

	@Query("""
		select task
		from Task task
		where (task.ownerUserId = :userId and task.roomId is null)
		   or task.assigneeUserId = :userId
		order by
		  case when task.dueAt is null then 1 else 0 end,
		  task.dueAt asc,
		  task.updatedAt desc
		""")
	Page<Task> findDashboardTasks(@Param("userId") UUID userId, Pageable pageable);

	@Query("""
		select task from Task task
		where ((task.ownerUserId = :userId and task.roomId is null) or task.assigneeUserId = :userId)
		  and task.dueAt >= :from and task.dueAt < :to
		  and task.status <> com.bubli.work.task.type.TaskStatus.DONE
		order by task.dueAt asc
		""")
	List<Task> findDueBetweenForUser(@Param("userId") UUID userId,
			@Param("from") Instant from,
			@Param("to") Instant to);

	boolean existsByWbsItemId(UUID wbsItemId);
}
