package com.bubli.resource.service;

import com.bubli.global.error.BusinessException;
import com.bubli.resource.dto.ResourceSearchHit;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ResourceSearchMetricsPublicService {

    private static final String METRIC_PREFIX = "bubli.ai.search";

    private final MeterRegistry meterRegistry;

    public List<ResourceSearchHit> observe(
            String strategy,
            String scope,
            Supplier<List<ResourceSearchHit>> search
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        try {
            List<ResourceSearchHit> hits = search.get();
            List<ResourceSearchHit> safeHits = hits == null ? List.of() : hits;
            outcome = safeHits.isEmpty() ? "empty" : "success";
            recordRequest(strategy, scope, outcome, "none");
            recordResults(strategy, scope, safeHits);
            return safeHits;
        } catch (RuntimeException exception) {
            recordRequest(strategy, scope, outcome, errorType(exception));
            throw exception;
        } finally {
            sample.stop(Timer.builder(METRIC_PREFIX + ".duration")
                    .description("AI resource search latency")
                    .tag("strategy", strategy)
                    .tag("scope", scope)
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }

    public void recordSelection(
            String strategy,
            String scope,
            int candidateCount,
            int acceptedCount
    ) {
        incrementCounter(METRIC_PREFIX + ".candidates", strategy, scope, candidateCount);
        incrementCounter(METRIC_PREFIX + ".accepted", strategy, scope, acceptedCount);
        incrementCounter(
                METRIC_PREFIX + ".rejected",
                strategy,
                scope,
                Math.max(0, candidateCount - acceptedCount)
        );
    }

    public void recordFallback(String strategy, String scope, boolean success) {
        Counter.builder(METRIC_PREFIX + ".fallback")
                .description("AI resource search fallback outcomes")
                .tag("strategy", strategy)
                .tag("scope", scope)
                .tag("outcome", success ? "success" : "empty")
                .register(meterRegistry)
                .increment();
    }

    public void recordFusion(
            String scope,
            int candidateCount,
            int selectedCount,
            boolean grounded,
            String primaryRetrievalMode
    ) {
        DistributionSummary.builder(METRIC_PREFIX + ".fusion.candidates")
                .description("Number of candidates considered by AI resource search fusion")
                .baseUnit("candidates")
                .tag("scope", scope)
                .tag("primary_retrieval_mode", safeTag(primaryRetrievalMode))
                .register(meterRegistry)
                .record(Math.max(0, candidateCount));
        DistributionSummary.builder(METRIC_PREFIX + ".fusion.selected")
                .description("Number of candidates selected by AI resource search fusion")
                .baseUnit("hits")
                .tag("scope", scope)
                .tag("primary_retrieval_mode", safeTag(primaryRetrievalMode))
                .register(meterRegistry)
                .record(Math.max(0, selectedCount));
        Counter.builder(METRIC_PREFIX + ".fusion.grounding")
                .description("AI resource search fusion grounding outcomes")
                .tag("scope", scope)
                .tag("primary_retrieval_mode", safeTag(primaryRetrievalMode))
                .tag("outcome", grounded ? "grounded" : "no_answer")
                .register(meterRegistry)
                .increment();
    }

    private void recordRequest(String strategy, String scope, String outcome, String errorType) {
        Counter.builder(METRIC_PREFIX + ".requests")
                .description("AI resource search requests")
                .tag("strategy", strategy)
                .tag("scope", scope)
                .tag("outcome", outcome)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    private void recordResults(String strategy, String scope, List<ResourceSearchHit> hits) {
        DistributionSummary.builder(METRIC_PREFIX + ".results")
                .description("Number of hits returned by an AI resource search")
                .baseUnit("hits")
                .tag("strategy", strategy)
                .tag("scope", scope)
                .register(meterRegistry)
                .record(hits.size());
        if (hits.isEmpty()) {
            return;
        }
        double maxScore = hits.stream()
                .mapToDouble(ResourceSearchHit::similarityScore)
                .max()
                .orElse(0.0D);
        DistributionSummary.builder(METRIC_PREFIX + ".max_score")
                .description("Maximum score returned by an AI resource search")
                .tag("strategy", strategy)
                .tag("scope", scope)
                .register(meterRegistry)
                .record(maxScore);
    }

    private void incrementCounter(String name, String strategy, String scope, int amount) {
        Counter.builder(name)
                .description("AI resource search grounding candidate selection")
                .tag("strategy", strategy)
                .tag("scope", scope)
                .register(meterRegistry)
                .increment(amount);
    }

    private String safeTag(String value) {
        return value == null || value.isBlank() ? "NONE" : value;
    }

    private String errorType(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            HttpStatus status = businessException.getErrorCode().getHttpStatus();
            if (status == HttpStatus.FORBIDDEN || status == HttpStatus.UNAUTHORIZED) {
                return "access_denied";
            }
            if (status == HttpStatus.BAD_REQUEST) {
                return "invalid_request";
            }
            if (status == HttpStatus.NOT_FOUND) {
                return "not_found";
            }
        }
        if (exception instanceof DataAccessException) {
            return "database";
        }
        String simpleName = exception.getClass().getSimpleName();
        if (simpleName.contains("Access") || simpleName.contains("Authorization")) {
            return "access_denied";
        }
        if (simpleName.contains("DataAccess") || simpleName.contains("Jdbc")) {
            return "database";
        }
        if (exception instanceof IllegalArgumentException) {
            return "invalid_request";
        }
        if (exception instanceof IllegalStateException) {
            return "dependency_unavailable";
        }
        String packageName = exception.getClass().getPackageName();
        if (packageName.startsWith("org.springframework.ai")
                || packageName.startsWith("software.amazon.awssdk")) {
            return "provider";
        }
        return "other";
    }
}
