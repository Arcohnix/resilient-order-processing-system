package com.sanket.resilientordersystem.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ExternalPaymentClient {

    @Value("${payment.simulate-timeout:false}")
    private volatile boolean simulateTimeout;

    @Value("${payment.simulate-timeout-ms:5000}")
    private long simulateTimeoutMs;

    private volatile boolean simulateFailure;

    public ExternalPaymentClient(
            @Value("${payment.simulate-failure:false}") boolean initialFailure
    ) {
        this.simulateFailure = initialFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulateTimeout(boolean simulateTimeout) {
        this.simulateTimeout = simulateTimeout;
    }

    public CompletableFuture<String> chargeAsync(Long orderId) {

        return CompletableFuture.supplyAsync(() -> {

            if (simulateFailure) {
                throw new RuntimeException(
                        "Simulated payment provider failure"
                );
            }

            if (simulateTimeout) {
                try {
                    Thread.sleep(simulateTimeoutMs);
                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                    throw new RuntimeException(
                            "Payment request interrupted",
                            exception
                    );
                }
            }

            return "PAY-" + UUID.randomUUID();
        });
    }
}