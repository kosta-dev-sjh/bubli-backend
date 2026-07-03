package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ResourceRelationRepository extends JpaRepository<ResourceRelation, UUID> {

	Page<ResourceRelation> findByResourceId(UUID resourceId, Pageable pageable);

	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query("""
			delete from ResourceRelation relation
			where relation.resourceId = :resourceId
			   or relation.relatedResourceId = :resourceId
			""")
	void deleteByResourceIdOrRelatedResourceId(@Param("resourceId") UUID resourceId, @Param("relatedResourceId") UUID relatedResourceId);
}
