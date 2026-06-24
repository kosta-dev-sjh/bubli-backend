package com.bubli.project.repository;

import com.bubli.project.entity.ProjectRoom;
import com.bubli.project.type.RoomMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectRoomRepository extends JpaRepository<ProjectRoom, UUID> {

	@Query(value = """
		select projectRoom
		from ProjectRoom projectRoom
		where exists (
			select 1
			from RoomMember roomMember
			where roomMember.roomId = projectRoom.id
			  and roomMember.userId = :userId
			  and roomMember.status = :status
		)
		order by projectRoom.createdAt desc
		""",
		countQuery = """
		select count(projectRoom)
		from ProjectRoom projectRoom
		where exists (
			select 1
			from RoomMember roomMember
			where roomMember.roomId = projectRoom.id
			  and roomMember.userId = :userId
			  and roomMember.status = :status
		)
		""")
	Page<ProjectRoom> findAccessibleRooms(@Param("userId") UUID userId,
			@Param("status") RoomMemberStatus status,
			Pageable pageable);
}
