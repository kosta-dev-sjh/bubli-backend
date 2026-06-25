package com.bubli.resource.service;

import com.bubli.resource.entity.Document;

import java.util.List;
import java.util.UUID;

public record DocumentVersionChange(
        Document newDocument,
        List<UUID> retiredVectorStoreIds
) {

    public DocumentVersionChange {
        retiredVectorStoreIds = List.copyOf(retiredVectorStoreIds);
    }
}
