package com.bubli.resource.repository;

import com.bubli.resource.entity.ResourceEmbedding;
import com.bubli.resource.type.ResourceVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResourceEmbeddingRepository extends JpaRepository<ResourceEmbedding, UUID> {

    List<ResourceEmbedding> findAllByResourceIdOrderByChunkIndex(UUID resourceId);

    void deleteAllByResourceId(UUID resourceId);

    List<ResourceEmbedding> findAllByOwnerIdAndVisibility(UUID ownerId, ResourceVisibility visibility);

    List<ResourceEmbedding> findAllByRoomIdAndVisibility(UUID roomId, ResourceVisibility visibility);
}
