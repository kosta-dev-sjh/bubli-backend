package com.bubli.memory.repository;

import com.bubli.memory.entity.DailySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.UUID;

public interface DailySummaryRepository extends JpaRepository<DailySummary, UUID> {

    Page<DailySummary> findByUserIdOrderBySummaryDateDesc(UUID userId, Pageable pageable);

    Optional<DailySummary> findByIdAndUserId(UUID id, UUID userId);

    Optional<DailySummary> findByUserIdAndSummaryDate(UUID userId, LocalDate summaryDate);
}
