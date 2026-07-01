package com.bubli.widget.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateWidgetBubbleRequest(
        @NotBlank String bubbleType,
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
