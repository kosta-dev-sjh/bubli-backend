package com.bubli.resource.service;

import com.bubli.resource.dto.ResourceSearchHit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceSearchMetricsPublicServiceTest {

    @Test
    void recordsSuccessfulSearchLatencyResultCountAndScore() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceSearchMetricsPublicService metrics = new ResourceSearchMetricsPublicService(registry);

        List<ResourceSearchHit> hits = metrics.observe("semantic", "room", () -> List.of(hit(0.91D)));

        assertThat(hits).hasSize(1);
        assertThat(registry.get("bubli.ai.search.requests")
                .tags("strategy", "semantic", "scope", "room", "outcome", "success", "error_type", "none")
                .counter().count()).isEqualTo(1.0D);
        assertThat(registry.get("bubli.ai.search.results")
                .tags("strategy", "semantic", "scope", "room")
                .summary().totalAmount()).isEqualTo(1.0D);
        assertThat(registry.get("bubli.ai.search.max_score")
                .tags("strategy", "semantic", "scope", "room")
                .summary().max()).isEqualTo(0.91D);
        assertThat(registry.get("bubli.ai.search.duration")
                .tags("strategy", "semantic", "scope", "room", "outcome", "success")
                .timer().count()).isEqualTo(1L);
    }

    @Test
    void recordsEmptySearchSeparately() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceSearchMetricsPublicService metrics = new ResourceSearchMetricsPublicService(registry);

        metrics.observe("keyword", "room", List::of);

        assertThat(registry.get("bubli.ai.search.requests")
                .tags("strategy", "keyword", "scope", "room", "outcome", "empty", "error_type", "none")
                .counter().count()).isEqualTo(1.0D);
        assertThat(registry.get("bubli.ai.search.results")
                .tags("strategy", "keyword", "scope", "room")
                .summary().totalAmount()).isZero();
    }

    @Test
    void recordsBoundedErrorTypeAndRethrowsFailure() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceSearchMetricsPublicService metrics = new ResourceSearchMetricsPublicService(registry);

        assertThatThrownBy(() -> metrics.observe("semantic", "personal", () -> {
            throw new IllegalStateException("embedding unavailable");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(registry.get("bubli.ai.search.requests")
                .tags(
                        "strategy", "semantic",
                        "scope", "personal",
                        "outcome", "error",
                        "error_type", "dependency_unavailable"
                )
                .counter().count()).isEqualTo(1.0D);
    }

    @Test
    void recordsThresholdAcceptedAndRejectedCandidates() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceSearchMetricsPublicService metrics = new ResourceSearchMetricsPublicService(registry);

        metrics.recordSelection("semantic", "room", 5, 2);

        assertThat(registry.get("bubli.ai.search.candidates")
                .tags("strategy", "semantic", "scope", "room")
                .counter().count()).isEqualTo(5.0D);
        assertThat(registry.get("bubli.ai.search.accepted")
                .tags("strategy", "semantic", "scope", "room")
                .counter().count()).isEqualTo(2.0D);
        assertThat(registry.get("bubli.ai.search.rejected")
                .tags("strategy", "semantic", "scope", "room")
                .counter().count()).isEqualTo(3.0D);
    }

    @Test
    void recordsFusionGroundingOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ResourceSearchMetricsPublicService metrics = new ResourceSearchMetricsPublicService(registry);

        metrics.recordFusion("room", 7, 3, true, "KEYWORD");

        assertThat(registry.get("bubli.ai.search.fusion.candidates")
                .tags("scope", "room", "primary_retrieval_mode", "KEYWORD")
                .summary().totalAmount()).isEqualTo(7.0D);
        assertThat(registry.get("bubli.ai.search.fusion.selected")
                .tags("scope", "room", "primary_retrieval_mode", "KEYWORD")
                .summary().totalAmount()).isEqualTo(3.0D);
        assertThat(registry.get("bubli.ai.search.fusion.grounding")
                .tags("scope", "room", "primary_retrieval_mode", "KEYWORD", "outcome", "grounded")
                .counter().count()).isEqualTo(1.0D);
    }

    private ResourceSearchHit hit(double score) {
        return new ResourceSearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                0,
                "chunk",
                1,
                1,
                1,
                0,
                5,
                "document.txt",
                "{}",
                score
        );
    }
}
