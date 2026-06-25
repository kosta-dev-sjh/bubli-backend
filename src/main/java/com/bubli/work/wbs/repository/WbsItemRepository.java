package com.bubli.work.wbs.repository;

import com.bubli.work.wbs.entity.WbsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface WbsItemRepository extends JpaRepository<WbsItem, UUID> {

	Page<WbsItem> findByRoomIdOrderByOrderNoAsc(UUID roomId, Pageable pageable);

	List<WbsItem> findByRoomIdOrderByParentIdAscOrderNoAsc(UUID roomId);

	List<WbsItem> findAllByIdIn(Collection<UUID> ids);

	boolean existsByParentId(UUID parentId);

	@Query("""
			select coalesce(max(w.orderNo), 0)
			from WbsItem w
			where w.roomId = :roomId
			  and (
			      (:parentId is null and w.parentId is null)
			      or w.parentId = :parentId
			  )
			""")
	int findMaxOrderNo(@Param("roomId") UUID roomId, @Param("parentId") UUID parentId);
}
