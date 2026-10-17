package com.sanket.resilientordersystem.order;

import com.sanket.resilientordersystem.inventory.InventoryService;
import com.sanket.resilientordersystem.observability.OrderMetrics;
import com.sanket.resilientordersystem.outbox.OutboxService;
import com.sanket.resilientordersystem.payment.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderMetrics orderMetrics;
    private final OutboxService outboxService;

    @Value("${order.simulate-crash-after-payment:false}")
    private boolean simulateCrashAfterPayment;

    public OrderService(
            OrderRepository orderRepository,
            IdempotencyKeyRepository idempotencyKeyRepository,
            InventoryService inventoryService,
            PaymentService paymentService,
            OrderMetrics orderMetrics,
            OutboxService outboxService
    ) {
        this.orderRepository = orderRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.orderMetrics = orderMetrics;
        this.outboxService = outboxService;
    }

    @Transactional
    public Order createOrder(
            String idempotencyKey,
            String productId,
            Integer quantity
    ) {

        return idempotencyKeyRepository.findByKey(idempotencyKey)
                .map(existingKey ->
                        orderRepository.findById(existingKey.getOrderId())
                                .orElseThrow()
                )
                .orElseGet(() -> {

                    inventoryService.reserve(productId, quantity);

                    Order order = new Order(productId, quantity);
                    order.setStatus(OrderStatus.PAYMENT_PENDING);

                    Order savedOrder = orderRepository.save(order);

                    try {

                        paymentService.processPayment(
                                savedOrder.getId(),
                                productId,
                                quantity
                        ).join();

                    } catch (Exception exception) {

                        inventoryService.release(
                                productId,
                                quantity
                        );

                        savedOrder.setStatus(OrderStatus.FAILED);

                        orderRepository.save(savedOrder);

                        IdempotencyKey key = new IdempotencyKey(
                                idempotencyKey,
                                savedOrder.getId()
                        );

                        idempotencyKeyRepository.save(key);

                        return savedOrder;
                    }

                    if (simulateCrashAfterPayment) {
                        throw new RuntimeException(
                                "Simulated crash after successful payment"
                        );
                    }

                    savedOrder.setStatus(OrderStatus.PAID);

                    IdempotencyKey key = new IdempotencyKey(
                            idempotencyKey,
                            savedOrder.getId()
                    );

                    idempotencyKeyRepository.save(key);

                    Order finalOrder =
                            orderRepository.save(savedOrder);

                    outboxService.createOrderCreatedEvent(finalOrder);

                    orderMetrics.orderCreated();

                    return finalOrder;
                });
    }
}