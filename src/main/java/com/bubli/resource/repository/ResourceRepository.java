package com.bubli.resource.repository;

import com.bubli.resource.entity.Resource;
import com.bubli.resource.type.ResourceVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
