package com.bubli.widget.dto;

import java.math.BigDecimal;

public record BubbleSettingUpdate(
        String bubbleType,
        Boolean enabled,
        Integer x,
        Integer y,
        Integer width,
        Integer height,
        Boolean minimized,
        BigDecimal opacity,
        Boolean ghostMode,
        Boolean alertEnabled
) {}
