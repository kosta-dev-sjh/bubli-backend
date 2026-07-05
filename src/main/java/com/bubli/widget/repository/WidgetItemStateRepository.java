package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetItemState;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetItemStateRepository extends JpaRepository<WidgetItemState, UUID> {

    Optional<WidgetItemState> findByUserIdAndBubbleTypeAndItemTypeAndItemId(
            UUID userId, BubbleType bubbleType, WidgetItemType itemType, UUID itemId);

    List<WidgetItemState> findByUserIdAndItemIdIn(UUID userId, List<UUID> itemIds);

    @Modifying
    @Query(value = """
            INSERT INTO widget_item_states (
                id, user_id, bubble_type, item_type, item_id, state, created_at, updated_at
            )
            VALUES (
                :id, :userId, :bubbleType, :itemType, :itemId, :state, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id, bubble_type, item_type, item_id) DO UPDATE
            SET state = EXCLUDED.state,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsertState(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("bubbleType") String bubbleType,
            @Param("itemType") String itemType,
            @Param("itemId") UUID itemId,
            @Param("state") String state
    );
}
