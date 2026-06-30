package com.bubli.widget.service;

import com.bubli.global.error.BusinessException;
import com.bubli.global.error.ErrorCode;
import com.bubli.widget.dto.BubbleSettingUpdate;
import com.bubli.widget.dto.WidgetBubbleSettingResponse;
import com.bubli.widget.dto.WidgetContextResponse;
import com.bubli.widget.dto.WidgetDailySummaryResponse;
import com.bubli.widget.dto.WidgetSettingsResponse;
import com.bubli.widget.dto.WidgetSummaryResponse;
import com.bubli.widget.dto.WidgetTodaySummaryResponse;
import com.bubli.widget.entity.WidgetBubbleSetting;
import com.bubli.widget.entity.WidgetContextSetting;
import com.bubli.widget.entity.WidgetDailySummary;
import com.bubli.widget.entity.WidgetItemState;
import com.bubli.widget.repository.WidgetBubbleSettingRepository;
import com.bubli.widget.repository.WidgetContextSettingRepository;
import com.bubli.widget.repository.WidgetDailySummaryRepository;
import com.bubli.widget.repository.WidgetItemStateRepository;
import com.bubli.widget.type.BubbleType;
import com.bubli.widget.type.WidgetItemStateValue;
import com.bubli.widget.type.WidgetItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WidgetService {

    private final WidgetBubbleSettingRepository bubbleSettingRepository;
    private final WidgetContextSettingRepository contextSettingRepository;
    private final WidgetItemStateRepository itemStateRepository;
    private final WidgetDailySummaryRepository dailySummaryRepository;

    @Transactional(readOnly = true)
    public WidgetSettingsResponse getSettings(UUID userId) {
        List<WidgetBubbleSettingResponse> bubbles = bubbleSettingRepository.findByUserId(userId)
                .stream().map(this::toSettingResponse).toList();
        return new WidgetSettingsResponse(bubbles);
    }

    @Transactional
    public WidgetSettingsResponse updateSettings(UUID userId, List<BubbleSettingUpdate> bubbles) {
        for (BubbleSettingUpdate req : bubbles) {
            BubbleType type = parseBubbleType(req.bubbleType());
            WidgetBubbleSetting setting = bubbleSettingRepository
                    .findByUserIdAndBubbleType(userId, type)
                    .orElseGet(() -> bubbleSettingRepository.save(WidgetBubbleSetting.create(userId, type)));
            setting.update(req.enabled(), req.x(), req.y(), req.width(), req.height(),
                    req.minimized(), req.opacity(), req.ghostMode(), req.alertEnabled());
        }
        return getSettings(userId);
    }

    @Transactional(readOnly = true)
    public WidgetContextResponse getContext(UUID userId) {
        return contextSettingRepository.findByUserId(userId)
                .map(c -> new WidgetContextResponse(c.getSelectedRoomId(), c.getMode().name()))
                .orElse(new WidgetContextResponse(null, "PERSONAL"));
    }

    @Transactional
    public WidgetContextResponse updateContext(UUID userId, UUID selectedRoomId) {
        WidgetContextSetting context = contextSettingRepository.findByUserId(userId)
                .orElseGet(() -> contextSettingRepository.save(WidgetContextSetting.create(userId, selectedRoomId)));
        context.updateRoom(selectedRoomId);
        return new WidgetContextResponse(context.getSelectedRoomId(), context.getMode().name());
    }

    @Transactional(readOnly = true)
    public WidgetSummaryResponse getSummary(UUID userId) {
        WidgetContextResponse context = getContext(userId);
        List<WidgetBubbleSettingResponse> bubbles = bubbleSettingRepository.findByUserId(userId)
                .stream().map(this::toSettingResponse).toList();
        return new WidgetSummaryResponse(context, bubbles);
    }

    @Transactional
    public void updateItemState(UUID userId, UUID itemStateId, String stateStr) {
        WidgetItemStateValue state = parseItemState(stateStr);
        WidgetItemState itemState = itemStateRepository.findById(itemStateId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.WIDGET_404_001));
        itemState.updateState(state);
    }

    @Transactional
    public WidgetDailySummaryResponse saveUsageSummary(UUID userId, String deviceId, String rollupKey,
            LocalDate summaryDate, UUID bubbleSettingId,
            int openCount, int interactionCount, long visibleSeconds, java.time.Instant syncedAt) {
        return dailySummaryRepository.findByRollupKey(rollupKey)
                .map(this::toSummaryResponse)
                .orElseGet(() -> {
                    WidgetDailySummary saved = dailySummaryRepository.save(
                            WidgetDailySummary.create(userId, deviceId, rollupKey,
                                    summaryDate, bubbleSettingId,
                                    openCount, interactionCount, visibleSeconds, syncedAt)
                    );
                    return toSummaryResponse(saved);
                });
    }

    @Transactional(readOnly = true)
    public WidgetTodaySummaryResponse getTodaySummary(UUID userId) {
        LocalDate today = LocalDate.now();
        List<WidgetDailySummary> summaries = dailySummaryRepository.findByUserIdAndSummaryDate(userId, today);
        int totalOpen = summaries.stream().mapToInt(WidgetDailySummary::getOpenCount).sum();
        int totalInteraction = summaries.stream().mapToInt(WidgetDailySummary::getInteractionCount).sum();
        long totalVisible = summaries.stream().mapToLong(WidgetDailySummary::getVisibleSeconds).sum();
        List<WidgetDailySummaryResponse> byDevice = summaries.stream().map(this::toSummaryResponse).toList();
        return new WidgetTodaySummaryResponse(today, totalOpen, totalInteraction, totalVisible, byDevice);
    }

    private WidgetBubbleSettingResponse toSettingResponse(WidgetBubbleSetting s) {
        return new WidgetBubbleSettingResponse(s.getId(), s.getBubbleType().name(),
                s.isEnabled(), s.getX(), s.getY(), s.getWidth(), s.getHeight(),
                s.isMinimized(), s.getOpacity(), s.isGhostMode(), s.isAlertEnabled());
    }

    private WidgetDailySummaryResponse toSummaryResponse(WidgetDailySummary s) {
        return new WidgetDailySummaryResponse(s.getId(), s.getDeviceId(), s.getSummaryDate(),
                s.getBubbleSettingId(), s.getOpenCount(), s.getInteractionCount(),
                s.getVisibleSeconds(), s.getSyncedAt());
    }

    private BubbleType parseBubbleType(String value) {
        try {
            return BubbleType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.WIDGET_400_001);
        }
    }

    private WidgetItemStateValue parseItemState(String value) {
        try {
            return WidgetItemStateValue.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.WIDGET_400_001);
        }
    }
}
