package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResourceCommentRepository extends JpaRepository<ResourceComment, UUID> {

	Page<ResourceComment> findByResourceIdAndDeletedAtIsNull(UUID resourceId, Pageable pageable);

	Optional<ResourceComment> findByIdAndDeletedAtIsNull(UUID id);
}
