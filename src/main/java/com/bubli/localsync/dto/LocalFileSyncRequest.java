package com.bubli.localsync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LocalFileSyncRequest(
        @NotEmpty @Size(max = 100) @Valid List<@NotNull LocalFileEvent> events
) {}
