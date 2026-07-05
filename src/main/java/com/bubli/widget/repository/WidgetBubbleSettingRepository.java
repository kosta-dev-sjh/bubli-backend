package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetBubbleSetting;
import com.bubli.widget.type.BubbleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetBubbleSettingRepository extends JpaRepository<WidgetBubbleSetting, UUID> {

    List<WidgetBubbleSetting> findByUserId(UUID userId);

    Optional<WidgetBubbleSetting> findByUserIdAndBubbleType(UUID userId, BubbleType bubbleType);

    @Modifying
    @Query(value = """
            INSERT INTO widget_bubble_settings (
                id, user_id, bubble_type, enabled, minimized, ghost_mode, alert_enabled, created_at, updated_at
            )
            VALUES (
                :id, :userId, :bubbleType, true, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id, bubble_type) DO NOTHING
            """, nativeQuery = true)
    int insertDefaultIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("bubbleType") String bubbleType
    );
}
