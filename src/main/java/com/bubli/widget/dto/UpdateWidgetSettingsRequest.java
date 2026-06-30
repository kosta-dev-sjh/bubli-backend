package com.bubli.widget.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateWidgetSettingsRequest(
        @NotEmpty @Valid List<UpdateWidgetBubbleRequest> bubbles
) {}
