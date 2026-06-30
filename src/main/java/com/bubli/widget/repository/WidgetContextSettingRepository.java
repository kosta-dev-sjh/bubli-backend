package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetContextSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WidgetContextSettingRepository extends JpaRepository<WidgetContextSetting, UUID> {

    Optional<WidgetContextSetting> findByUserId(UUID userId);
}
