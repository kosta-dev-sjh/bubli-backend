package com.bubli.localsync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LocalFileSyncRequest(
        @NotEmpty @Valid List<LocalFileEvent> events
) {}
