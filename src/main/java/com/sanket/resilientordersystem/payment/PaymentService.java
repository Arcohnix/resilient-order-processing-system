package com.sanket.resilientordersystem.payment;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private final ExternalPaymentClient externalPaymentClient;
    private final PaymentPersistenceService paymentPersistenceService;

    public PaymentService(
            ExternalPaymentClient externalPaymentClient,
            PaymentPersistenceService paymentPersistenceService
    ) {
        this.externalPaymentClient = externalPaymentClient;
        this.paymentPersistenceService = paymentPersistenceService;
    }

    @Retry(name = "paymentRetry")
    @CircuitBreaker(
            name = "paymentCircuitBreaker",
            fallbackMethod = "paymentFallback"
    )
    @TimeLimiter(name = "paymentTimeout")
    public CompletableFuture<Payment> processPayment(
            Long orderId,
            String productId,
            Integer quantity
    ) {

        return externalPaymentClient
                .chargeAsync(orderId)
                .thenApply(paymentId ->
                        paymentPersistenceService.savePayment(
                                orderId,
                                productId,
                                quantity,
                                paymentId
                        )
                );
    }

    private CompletableFuture<Payment> paymentFallback(
            Long orderId,
            String productId,
            Integer quantity,
            Throwable exception
    ) {

        return CompletableFuture.failedFuture(
                new IllegalStateException(
                        "Payment service unavailable for order: "
                                + orderId,
                        exception
                )
        );
    }
}