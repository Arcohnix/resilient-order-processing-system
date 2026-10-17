package com.sanket.resilientordersystem.recovery;

import com.sanket.resilientordersystem.inventory.InventoryService;
import com.sanket.resilientordersystem.order.Order;
import com.sanket.resilientordersystem.order.OrderRepository;
import com.sanket.resilientordersystem.order.OrderStatus;
import com.sanket.resilientordersystem.payment.Payment;
import com.sanket.resilientordersystem.payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecoveryService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public RecoveryService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            InventoryService inventoryService
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    public List<Payment> findOrphanedPayments() {

        return paymentRepository.findAll()
                .stream()
                .filter(payment ->
                        payment.getStatus().name().equals("SUCCESS")
                                && payment.getRecoveredOrderId() == null
                                && orderRepository
                                .findById(payment.getOrderId())
                                .isEmpty()
                )
                .toList();
    }

    @Transactional
    public Order recoverPayment(Long orderId) {

        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found for order: " + orderId
                        )
                );

        if (!payment.getStatus().name().equals("SUCCESS")) {
            throw new IllegalStateException(
                    "Payment is not successful for order: " + orderId
            );
        }

        if (payment.getRecoveredOrderId() != null) {
            return orderRepository
                    .findById(payment.getRecoveredOrderId())
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Recovered order not found: "
                                            + payment.getRecoveredOrderId()
                            )
                    );
        }

        inventoryService.reserve(
                payment.getProductId(),
                payment.getQuantity()
        );

        Order recoveredOrder = new Order(
                payment.getProductId(),
                payment.getQuantity()
        );

        recoveredOrder.setStatus(OrderStatus.PAID);

        Order savedOrder = orderRepository.save(recoveredOrder);

        payment.setRecoveredOrderId(savedOrder.getId());

        paymentRepository.save(payment);

        return savedOrder;
    }
}