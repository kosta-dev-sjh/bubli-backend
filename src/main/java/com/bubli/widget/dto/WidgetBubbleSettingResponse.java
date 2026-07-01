package com.bubli.widget.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WidgetBubbleSettingResponse(
        UUID id,
        String bubbleType,
        boolean enabled,
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        boolean minimized,
        BigDecimal opacity,
        boolean ghostMode,
        boolean alertEnabled
) {}
