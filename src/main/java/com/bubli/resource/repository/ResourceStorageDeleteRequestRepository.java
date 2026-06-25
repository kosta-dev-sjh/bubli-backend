package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import com.bubli.resource.type.ResourceStorageDeleteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface ResourceStorageDeleteRequestRepository extends JpaRepository<ResourceStorageDeleteRequest, UUID> {

	Page<ResourceStorageDeleteRequest> findByStatusIn(
			Collection<ResourceStorageDeleteStatus> statuses,
			Pageable pageable
	);
}
