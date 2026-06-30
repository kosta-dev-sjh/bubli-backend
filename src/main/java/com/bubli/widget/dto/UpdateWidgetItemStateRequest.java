package com.bubli.widget.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWidgetItemStateRequest(@NotBlank String state) {}
