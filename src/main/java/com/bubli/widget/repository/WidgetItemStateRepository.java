package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetItemState;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WidgetItemStateRepository extends JpaRepository<WidgetItemState, UUID> {

    Optional<WidgetItemState> findByUserIdAndBubbleTypeAndItemTypeAndItemId(
            UUID userId, BubbleType bubbleType, WidgetItemType itemType, UUID itemId);
}
