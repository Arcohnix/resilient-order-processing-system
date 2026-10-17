package com.sanket.resilientordersystem.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InventoryMetrics {

    private final Counter reservationsSuccessful;
    private final Counter reservationsRejected;

    public InventoryMetrics(MeterRegistry meterRegistry) {

        reservationsSuccessful = Counter.builder("inventory.reservations.successful")
                .description("Number of successful inventory reservations")
                .register(meterRegistry);

        reservationsRejected = Counter.builder("inventory.reservations.rejected")
                .description("Number of rejected inventory reservations")
                .register(meterRegistry);
    }

    public void reservationSuccessful() {
        reservationsSuccessful.increment();
    }

    public void reservationRejected() {
        reservationsRejected.increment();
    }
}