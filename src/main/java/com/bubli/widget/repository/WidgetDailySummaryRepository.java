package com.bubli.widget.repository;

import com.bubli.widget.entity.WidgetDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetDailySummaryRepository extends JpaRepository<WidgetDailySummary, UUID> {

    Optional<WidgetDailySummary> findByRollupKey(String rollupKey);

    List<WidgetDailySummary> findByUserIdAndSummaryDate(UUID userId, LocalDate summaryDate);
}
