package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResourceFileRepository extends JpaRepository<ResourceFile, UUID> {

    @Query("""
            select count(rf) > 0
            from ResourceFile rf
            join Resource r on r.id = rf.resourceId
            where r.roomId = :roomId
              and rf.checksum = :checksum
              and r.status <> com.bubli.resource.type.ResourceStatus.FAILED
            """)
    boolean existsActiveRoomFileByChecksum(
            @Param("roomId") UUID roomId,
            @Param("checksum") String checksum
    );

    java.util.Optional<ResourceFile> findTopByResourceIdOrderByCreatedAtDesc(UUID resourceId);

    List<ResourceFile> findByResourceId(UUID resourceId);
}
