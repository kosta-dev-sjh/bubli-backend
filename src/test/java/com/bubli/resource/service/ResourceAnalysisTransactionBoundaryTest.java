package com.bubli.resource.service;

import com.bubli.agent.dispatch.AgentResourceAnalysisCompletionRecorder;
import com.bubli.agent.dispatch.AgentResourceAnalysisCompletionWriter;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceAnalysisTransactionBoundaryTest {

    @Test
    void remotePreparationRunsBeforeAnyCompletionTransaction() {
        assertThat(transactional(method(ResourceEmbeddingIndexPublicService.class, "prepare"))).isNull();
        assertThat(transactional(method(ResourceAnalysisPublicService.class, "prepareEmbeddingIndex"))).isNull();
        assertThat(transactional(method(ResourceAnalysisPublicService.class, "completeAnalysisForJob"))).isNull();
        assertThat(transactional(method(AgentResourceAnalysisCompletionRecorder.class, "complete"))).isNull();
    }

    @Test
    void databaseReplacementRequiresTheShortCompletionTransaction() {
        Transactional replacement = transactional(method(ResourceEmbeddingIndexPublicService.class, "replace"));
        assertThat(replacement).isNotNull();
        assertThat(replacement.propagation()).isEqualTo(Propagation.MANDATORY);

        assertThat(transactional(method(ResourceAnalysisCompletionWriter.class, "complete"))).isNotNull();
        assertThat(transactional(method(AgentResourceAnalysisCompletionWriter.class, "complete"))).isNotNull();
    }

    private Method method(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private Transactional transactional(Method method) {
        return AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
    }
}
