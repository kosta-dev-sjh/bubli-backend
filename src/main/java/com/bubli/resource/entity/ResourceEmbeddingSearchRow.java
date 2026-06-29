package com.bubli.resource.entity;

import java.util.UUID;

public interface ResourceEmbeddingSearchRow {

    UUID getId();

    UUID getResourceId();

    int getChunkIndex();

    String getChunkText();

    String getChunkMetadata();

    double getSimilarityScore();
}
