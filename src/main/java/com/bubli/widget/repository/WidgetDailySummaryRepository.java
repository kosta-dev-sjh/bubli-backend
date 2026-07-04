package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetDailySummaryRepository extends JpaRepository<WidgetDailySummary, UUID> {

    Optional<WidgetDailySummary> findByRollupKey(String rollupKey);

    Optional<WidgetDailySummary> findByUserIdAndDeviceIdAndSummaryDateAndBubbleSettingId(
            UUID userId, String deviceId, LocalDate summaryDate, UUID bubbleSettingId);

    List<WidgetDailySummary> findByUserIdAndSummaryDate(UUID userId, LocalDate summaryDate);

    @Modifying
    @Query(value = """
            INSERT INTO widget_daily_summaries (
                id, user_id, device_id, rollup_key, summary_date, bubble_setting_id,
                open_count, interaction_count, visible_seconds, synced_at, created_at, updated_at
            )
            VALUES (
                :id, :userId, :deviceId, :rollupKey, :summaryDate, :bubbleSettingId,
                :openCount, :interactionCount, :visibleSeconds, :syncedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("deviceId") String deviceId,
            @Param("rollupKey") String rollupKey,
            @Param("summaryDate") LocalDate summaryDate,
            @Param("bubbleSettingId") UUID bubbleSettingId,
            @Param("openCount") int openCount,
            @Param("interactionCount") int interactionCount,
            @Param("visibleSeconds") long visibleSeconds,
            @Param("syncedAt") java.time.Instant syncedAt
    );
}
