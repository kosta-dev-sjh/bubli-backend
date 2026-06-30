package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetBubbleSetting;
import com.bubli.widget.type.BubbleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetBubbleSettingRepository extends JpaRepository<WidgetBubbleSetting, UUID> {

    List<WidgetBubbleSetting> findByUserId(UUID userId);

    Optional<WidgetBubbleSetting> findByUserIdAndBubbleType(UUID userId, BubbleType bubbleType);
}
