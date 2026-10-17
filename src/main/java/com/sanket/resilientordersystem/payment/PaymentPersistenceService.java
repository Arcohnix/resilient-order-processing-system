package com.sanket.resilientordersystem.payment;

import com.sanket.resilientordersystem.observability.PaymentMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentPersistenceService {

    private final PaymentRepository paymentRepository;
    private final PaymentMetrics paymentMetrics;

    public PaymentPersistenceService(
            PaymentRepository paymentRepository,
            PaymentMetrics paymentMetrics
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMetrics = paymentMetrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment savePayment(
            Long orderId,
            String productId,
            Integer quantity,
            String paymentId
    ) {

        return paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> {

                    Payment payment = new Payment(
                            orderId,
                            productId,
                            quantity,
                            paymentId,
                            PaymentStatus.SUCCESS
                    );

                    Payment savedPayment =
                            paymentRepository.save(payment);

                    paymentMetrics.paymentSuccessful();

                    return savedPayment;
                });
    }
}