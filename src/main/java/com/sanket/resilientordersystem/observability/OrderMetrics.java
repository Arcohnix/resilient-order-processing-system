package com.sanket.resilientordersystem.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OrderMetrics {

    private final Counter ordersCreated;

    public OrderMetrics(MeterRegistry meterRegistry) {
        this.ordersCreated = Counter.builder("orders_created")
                .description("Number of orders created")
                .register(meterRegistry);
    }

    public void orderCreated() {
        ordersCreated.increment();
    }
}