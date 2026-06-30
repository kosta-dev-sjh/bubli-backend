package com.bubli.widget.dto;

import java.util.UUID;

public record WidgetContextResponse(
        UUID selectedRoomId,
        String mode
) {}
