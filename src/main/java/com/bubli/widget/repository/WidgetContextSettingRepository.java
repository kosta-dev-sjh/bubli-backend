package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetContextSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WidgetContextSettingRepository extends JpaRepository<WidgetContextSetting, UUID> {

    Optional<WidgetContextSetting> findByUserId(UUID userId);

    @Modifying
    @Query(value = """
            INSERT INTO widget_context_settings (
                id, user_id, selected_room_id, mode, created_at, updated_at
            )
            VALUES (
                :id, :userId, :selectedRoomId, :mode, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            )
            ON CONFLICT (user_id) DO UPDATE
            SET selected_room_id = EXCLUDED.selected_room_id,
                mode = EXCLUDED.mode,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    int upsertContext(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("selectedRoomId") UUID selectedRoomId,
            @Param("mode") String mode
    );
}
