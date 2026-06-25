package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceStorageDeleteRetryRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResourceStorageDeleteRetryRecordRepository extends JpaRepository<ResourceStorageDeleteRetryRecord, UUID> {
}
