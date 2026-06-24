package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceRelation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResourceRelationRepository extends JpaRepository<ResourceRelation, UUID> {

	Page<ResourceRelation> findByResourceId(UUID resourceId, Pageable pageable);
}
