package com.sanket.resilientordersystem.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceMetricsConfig {

    public ResilienceMetricsConfig(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        TaggedCircuitBreakerMetrics
                .ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(
                        io.micrometer.core.instrument.Metrics.globalRegistry
                );

        TaggedRetryMetrics
                .ofRetryRegistry(retryRegistry)
                .bindTo(
                        io.micrometer.core.instrument.Metrics.globalRegistry
                );
    }
}