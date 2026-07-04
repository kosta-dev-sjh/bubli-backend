package com.bubli.work.task.repository;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.entity.RoomMember;
import com.bubli.project.repository.ProjectRoomRepository;
import com.bubli.project.repository.RoomMemberRepository;
import com.bubli.support.PostgresIntegrationTestSupport;
import com.bubli.user.entity.User;
import com.bubli.user.repository.UserRepository;
import com.bubli.work.task.entity.Task;
import com.bubli.work.task.type.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@Transactional
class TaskRepositoryIntegrationTest extends PostgresIntegrationTestSupport {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProjectRoomRepository projectRoomRepository;

	@Autowired
	RoomMemberRepository roomMemberRepository;

	@Autowired
	TaskRepository taskRepository;

	@Test
	void userVisibleTaskQueriesKeepRoomTasksLimitedToActiveMembership() {
		User user = userRepository.save(User.createGoogleUser(
				"google-sub-task-visible",
				"task-visible",
				"민서",
				null,
				"ko",
				"Asia/Seoul"
		));
		ProjectRoom activeRoom = saveRoom(user, "활성 룸");
		ProjectRoom leftRoom = saveRoom(user, "나간 룸");
		ProjectRoom removedRoom = saveRoom(user, "제거된 룸");
		ProjectRoom noMemberRoom = saveRoom(user, "멤버 없는 룸");
		roomMemberRepository.save(RoomMember.createMember(activeRoom.getId(), user.getId()));
		RoomMember leftMember = RoomMember.createMember(leftRoom.getId(), user.getId());
		leftMember.leave();
		roomMemberRepository.save(leftMember);
		RoomMember removedMember = RoomMember.createMember(removedRoom.getId(), user.getId());
		removedMember.remove();
		roomMemberRepository.save(removedMember);
		Instant dueAt = Instant.parse("2026-07-05T09:00:00Z");

		taskRepository.saveAll(List.of(
				Task.createPersonal(user.getId(), "개인 할 일", null, TaskStatus.TODO, dueAt),
				Task.createRoomTask(activeRoom.getId(), user.getId(), null, "활성 룸 할 일", null, TaskStatus.TODO, dueAt),
				Task.createRoomTask(leftRoom.getId(), user.getId(), null, "나간 룸 할 일", null, TaskStatus.TODO, dueAt),
				Task.createRoomTask(removedRoom.getId(), user.getId(), null, "제거된 룸 할 일", null, TaskStatus.TODO, dueAt),
				Task.createRoomTask(noMemberRoom.getId(), user.getId(), null, "멤버 없는 룸 할 일", null, TaskStatus.TODO, dueAt)
		));

		assertThat(taskRepository.findVisibleTasksForUser(user.getId(), Pageable.unpaged()).getContent())
				.extracting(Task::getTitle)
				.containsExactlyInAnyOrder("개인 할 일", "활성 룸 할 일");
		assertThat(taskRepository.findDashboardTasks(user.getId(), Pageable.unpaged()).getContent())
				.extracting(Task::getTitle)
				.containsExactlyInAnyOrder("개인 할 일", "활성 룸 할 일");
		assertThat(taskRepository.findAssignedVisibleTasksForUser(user.getId(), Pageable.unpaged()).getContent())
				.extracting(Task::getTitle)
				.containsExactly("활성 룸 할 일");
		assertThat(taskRepository.findDueBetweenForUser(
				user.getId(),
				Instant.parse("2026-07-05T00:00:00Z"),
				Instant.parse("2026-07-06T00:00:00Z")
		))
				.extracting(Task::getTitle)
				.containsExactlyInAnyOrder("개인 할 일", "활성 룸 할 일");
	}

	private ProjectRoom saveRoom(User user, String name) {
		return projectRoomRepository.save(ProjectRoom.create(
				user.getId(),
				name,
				null,
				null,
				null,
				null,
				null,
				null
		));
	}
}
