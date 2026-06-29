package com.bubli.resource.service;

import org.springframework.stereotype.Component;

//차원 수 불일치(1024가 아닌 경우)나 NaN/Infinity 거름망
//pgVector 문자열로 변환[v1,v2,v3,...]

@Component
public class EmbeddingVectorFormatter {

    private static final int EXPECTED_EMBEDDING_DIMENSIONS = 1024;

    public String toVectorLiteral(float[] embedding) {
        if (embedding == null || embedding.length != EXPECTED_EMBEDDING_DIMENSIONS) {
            throw new IllegalArgumentException("Embedding dimensions must be " + EXPECTED_EMBEDDING_DIMENSIONS + ".");
        }

        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            float value = embedding[index];
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding contains a non-finite value.");
            }
            if (index > 0) {
                builder.append(',');
            }
            builder.append(Float.toString(value));
        }
        return builder.append(']').toString();
    }
}
