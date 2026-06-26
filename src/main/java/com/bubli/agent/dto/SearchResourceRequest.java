package com.bubli.agent.dto;

import com.bubli.resource.type.ResourceSearchScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record SearchResourceRequest(
        ResourceSearchScope scope,
        UUID roomId,
        @NotBlank String query,
        @Positive Integer topK
) {

    public SearchResourceRequest {
        scope = scope == null ? ResourceSearchScope.ROOM_SHARED : scope;
    }
}
