package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceSummary;
import com.bubli.resource.type.AnalysisStatus;
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

    @Query("""
            select summary
            from ResourceSummary summary
            join Resource resource on resource.id = summary.resourceId
            where resource.ownerId = :ownerId
              and resource.deletedAt is null
              and summary.status = :status
              and summary.summaryJson is not null
            order by summary.updatedAt desc, summary.id desc
            """)
    Page<ResourceSummary> findRecentOwnerAnalyzedSummaries(
            @Param("ownerId") UUID ownerId,
            @Param("status") AnalysisStatus status,
            Pageable pageable
    );

    @Query("""
            select summary
            from ResourceSummary summary
            join Resource resource on resource.id = summary.resourceId
            join ResourceFile file on file.resourceId = resource.id
            where summary.resourceId <> :resourceId
              and file.checksum = :checksum
              and resource.visibility = :visibility
              and (
                    (:roomId is null and resource.roomId is null)
                    or resource.roomId = :roomId
                  )
              and resource.deletedAt is null
              and summary.summaryJson is not null
            order by summary.updatedAt desc, summary.id desc
            """)
    Page<ResourceSummary> findReusableAnalysisSummaries(
            @Param("resourceId") UUID resourceId,
            @Param("checksum") String checksum,
            @Param("roomId") UUID roomId,
            @Param("visibility") ResourceVisibility visibility,
            Pageable pageable
    );
}
