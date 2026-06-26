package com.bubli.agent.dto;

import com.bubli.resource.dto.ResourceSearchHit;

import java.util.List;

public record SearchResourceResponse(
        List<ResourceSearchHit> hits
) {

    public static SearchResourceResponse of(List<ResourceSearchHit> hits) {
        return new SearchResourceResponse(hits);
    }
}
