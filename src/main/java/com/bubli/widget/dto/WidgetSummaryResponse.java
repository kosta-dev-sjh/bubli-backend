package com.bubli.widget.dto;

import java.util.List;

public record WidgetSummaryResponse(
        WidgetContextResponse context,
        List<WidgetBubbleSettingResponse> bubbles
) {}
