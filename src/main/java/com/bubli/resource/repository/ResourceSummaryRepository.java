package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.type.ResourceVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ResourceSummaryRepository extends JpaRepository<ResourceSummary, UUID> {

    Optional<ResourceSummary> findTopByResourceIdOrderByCreatedAtDesc(UUID resourceId);

    Optional<ResourceSummary> findFirstByResourceIdOrderByUpdatedAtDescIdDesc(UUID resourceId);

    @Query("""
            select summary
            from ResourceSummary summary
            join Resource resource on resource.id = summary.resourceId
            where resource.roomId = :roomId
              and resource.visibility = :visibility
              and resource.deletedAt is null
            order by summary.updatedAt desc, summary.id desc
            """)
    Page<ResourceSummary> findRecentRoomSummaries(
            @Param("roomId") UUID roomId,
            @Param("visibility") ResourceVisibility visibility,
            Pageable pageable
    );
}
