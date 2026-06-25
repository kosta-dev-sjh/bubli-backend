package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResourceSummaryRepository extends JpaRepository<ResourceSummary, UUID> {

    Optional<ResourceSummary> findTopByResourceIdOrderByCreatedAtDesc(UUID resourceId);
}
