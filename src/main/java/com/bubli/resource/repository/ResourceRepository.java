package com.bubli.resource.repository;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.type.ResourceKind;
import com.bubli.resource.type.ResourceStatus;
import com.bubli.resource.type.ResourceVisibility;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

	Page<Resource> findByOwnerIdAndVisibilityAndDeletedAtIsNull(
			UUID ownerId,
			ResourceVisibility visibility,
			Pageable pageable
	);

	Page<Resource> findByRoomIdAndVisibilityAndDeletedAtIsNull(
			UUID roomId,
			ResourceVisibility visibility,
			Pageable pageable
	);

	Optional<Resource> findByIdAndDeletedAtIsNull(UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select resource
			from Resource resource
			where resource.id = :resourceId
			""")
	Optional<Resource> lockById(@Param("resourceId") UUID resourceId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Resource r where r.id = :resourceId and r.ownerId = :ownerId and r.visibility = com.bubli.resource.type.ResourceVisibility.PERSONAL")
	Optional<Resource> lockPersonalByIdAndOwnerId(@Param("resourceId") UUID resourceId, @Param("ownerId") UUID ownerId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select r from Resource r where r.id = :resourceId and (
			(r.visibility = com.bubli.resource.type.ResourceVisibility.PERSONAL and r.ownerId = :userId)
			or (r.visibility = com.bubli.resource.type.ResourceVisibility.ROOM_SHARED and exists (
			select m.id from RoomMember m where m.roomId = r.roomId and m.userId = :userId
			and m.status = com.bubli.project.type.RoomMemberStatus.ACTIVE)))
			""")
	Optional<Resource> lockReadableByIdAndUserId(@Param("resourceId") UUID resourceId, @Param("userId") UUID userId);

	Page<Resource> findByRoomIdAndVisibilityAndKindAndDeletedAtIsNullAndStatusIn(
			UUID roomId,
			ResourceVisibility visibility,
			ResourceKind kind,
			Collection<ResourceStatus> statuses,
			Pageable pageable
	);

	@Query("""
			select resource
			from Resource resource
			where resource.roomId = :roomId
			  and resource.visibility = :visibility
			  and resource.deletedAt is null
			  and resource.status in :statuses
			  and (
					lower(resource.title) like lower(concat('%', :keyword1, '%'))
					or lower(resource.title) like lower(concat('%', :keyword2, '%'))
					or lower(resource.title) like lower(concat('%', :keyword3, '%'))
			  )
			order by resource.createdAt desc, resource.id desc
			""")
	Page<Resource> findLatestRoomResourceCandidates(
			@Param("roomId") UUID roomId,
			@Param("visibility") ResourceVisibility visibility,
			@Param("statuses") Collection<ResourceStatus> statuses,
			@Param("keyword1") String keyword1,
			@Param("keyword2") String keyword2,
			@Param("keyword3") String keyword3,
			Pageable pageable
	);

	@Query("""
			select resource.createdAt
			from Resource resource
			where resource.ownerId = :ownerId
			  and resource.deletedAt is null
			  and resource.createdAt >= :from and resource.createdAt < :to
			""")
	List<Instant> findCreatedAtByOwnerIdAndCreatedAtBetween(
			@Param("ownerId") UUID ownerId,
			@Param("from") Instant from,
			@Param("to") Instant to
	);
}
