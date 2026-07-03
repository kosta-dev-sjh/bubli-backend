package com.bubli.widget.dto;

import com.bubli.widget.entity.WidgetItemState;

import java.util.UUID;

public record WidgetItemStateResponse(
        UUID id,
        String bubbleType,
        String itemType,
        UUID itemId,
        String state
) {
    public static WidgetItemStateResponse from(WidgetItemState itemState) {
        return new WidgetItemStateResponse(
                itemState.getId(),
                itemState.getBubbleType().name(),
                itemState.getItemType().name(),
                itemState.getItemId(),
                itemState.getState().name()
        );
    }
}
