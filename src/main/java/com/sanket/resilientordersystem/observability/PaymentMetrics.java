package com.sanket.resilientordersystem.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter paymentsSuccessful;
    private final Counter paymentsFailed;

    public PaymentMetrics(MeterRegistry meterRegistry) {

        paymentsSuccessful = Counter.builder("payments.successful")
                .description("Number of successful payments")
                .register(meterRegistry);

        paymentsFailed = Counter.builder("payments.failed")
                .description("Number of failed payments")
                .register(meterRegistry);
    }

    public void paymentSuccessful() {
        paymentsSuccessful.increment();
    }

    public void paymentFailed() {
        paymentsFailed.increment();
    }
}