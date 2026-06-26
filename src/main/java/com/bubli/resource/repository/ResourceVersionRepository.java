package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ResourceVersionRepository extends JpaRepository<ResourceVersion, UUID> {

	Page<ResourceVersion> findByResourceId(UUID resourceId, Pageable pageable);

	Optional<ResourceVersion> findFirstByResourceIdOrderByVersionNoDescIdDesc(UUID resourceId);

	@Query("""
			select coalesce(max(version.versionNo), 0)
			from ResourceVersion version
			where version.resourceId = :resourceId
			""")
	int findMaxVersionNo(@Param("resourceId") UUID resourceId);
}
