package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceStorageDeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResourceStorageDeleteRequestRepository extends JpaRepository<ResourceStorageDeleteRequest, UUID> {
}
