package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResourceVersionRepository extends JpaRepository<ResourceVersion, UUID> {
}
