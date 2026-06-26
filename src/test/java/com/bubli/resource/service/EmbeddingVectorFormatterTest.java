package com.bubli.resource.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingVectorFormatterTest {

    private final EmbeddingVectorFormatter formatter = new EmbeddingVectorFormatter();

    @Test
    void formatsValidVectorLiteral() {
        assertThat(formatter.toVectorLiteral(vector(0.25f))).startsWith("[0.25,0.25").endsWith("]");
    }

    @Test
    void rejectsInvalidDimensions() {
        assertThatThrownBy(() -> formatter.toVectorLiteral(new float[] {0.1f}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Embedding dimensions");
    }

    @Test
    void rejectsNonFiniteValues() {
        float[] vector = vector(0.1f);
        vector[3] = Float.NaN;

        assertThatThrownBy(() -> formatter.toVectorLiteral(vector))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-finite");
    }

    @Test
    void rejectsNullVector() {
        assertThatThrownBy(() -> formatter.toVectorLiteral(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Embedding dimensions");
    }

    private float[] vector(float value) {
        float[] vector = new float[1024];
        java.util.Arrays.fill(vector, value);
        return vector;
    }
}
