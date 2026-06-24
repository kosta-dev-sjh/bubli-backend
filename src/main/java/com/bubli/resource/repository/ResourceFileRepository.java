package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceFileRepository extends JpaRepository<ResourceFile, UUID> {

	List<ResourceFile> findByResourceId(UUID resourceId);
}
