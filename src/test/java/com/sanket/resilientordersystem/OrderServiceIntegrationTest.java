package com.sanket.resilientordersystem;

import com.sanket.resilientordersystem.inventory.Inventory;
import com.sanket.resilientordersystem.inventory.InventoryRepository;
import com.sanket.resilientordersystem.order.IdempotencyKeyRepository;
import com.sanket.resilientordersystem.order.Order;
import com.sanket.resilientordersystem.order.OrderRepository;
import com.sanket.resilientordersystem.order.OrderService;
import com.sanket.resilientordersystem.order.OrderStatus;
import com.sanket.resilientordersystem.outbox.OrderEventConsumer;
import com.sanket.resilientordersystem.outbox.OutboxEvent;
import com.sanket.resilientordersystem.outbox.OutboxEventRepository;
import com.sanket.resilientordersystem.outbox.OutboxStatus;
import com.sanket.resilientordersystem.outbox.ProcessedEventRepository;
import com.sanket.resilientordersystem.payment.ExternalPaymentClient;
import com.sanket.resilientordersystem.payment.Payment;
import com.sanket.resilientordersystem.payment.PaymentRepository;
import com.sanket.resilientordersystem.payment.PaymentStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private OrderEventConsumer orderEventConsumer;

    @Autowired
    private ExternalPaymentClient externalPaymentClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        resetState();
    }

    @AfterEach
    void tearDown() {
        resetState();
    }

    private void resetState() {
        externalPaymentClient.setSimulateFailure(false);
        externalPaymentClient.setSimulateTimeout(false);

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("paymentCircuitBreaker");
        circuitBreaker.reset();

        idempotencyKeyRepository.deleteAll();
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();
        processedEventRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();

        inventoryRepository.save(
                new Inventory("PROD-001", 10)
        );
    }

    @Test
    void sameIdempotencyKeyShouldReturnSameOrderAndNotDeductInventoryTwice() {

        Order firstOrder = orderService.createOrder(
                "test-idempotency-key",
                "PROD-001",
                1
        );

        Order secondOrder = orderService.createOrder(
                "test-idempotency-key",
                "PROD-001",
                1
        );

        assertEquals(firstOrder.getId(), secondOrder.getId());
        assertEquals(OrderStatus.PAID, firstOrder.getStatus());
        assertEquals(OrderStatus.PAID, secondOrder.getStatus());

        Inventory inventory = inventoryRepository
                .findByProductId("PROD-001")
                .orElseThrow();

        assertEquals(9, inventory.getQuantity());
    }

    @Test
    void insufficientInventoryShouldFailOrderCreationAndKeepInventoryUnchanged() {

        inventoryRepository.deleteAll();
        inventoryRepository.save(new Inventory("PROD-001", 1));

        assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(
                        "out-of-stock-key",
                        "PROD-001",
                        2
                )
        );

        Inventory inventory = inventoryRepository
                .findByProductId("PROD-001")
                .orElseThrow();

        assertEquals(1, inventory.getQuantity());
        assertTrue(paymentRepository.findAll().isEmpty());
    }

    @Test
    void failedPaymentShouldReleaseInventoryAndFailOrder() {

        externalPaymentClient.setSimulateFailure(true);

        Order order = orderService.createOrder(
                "payment-failure-test",
                "PROD-001",
                2
        );

        assertEquals(OrderStatus.FAILED, order.getStatus());

        Inventory inventory = inventoryRepository
                .findByProductId("PROD-001")
                .orElseThrow();

        assertEquals(10, inventory.getQuantity());
    }

    @Test
    void successfulOrderShouldBeMarkedAsPaidAndRecordSuccessfulPayment() {

        Order order = orderService.createOrder(
                "successful-order-test",
                "PROD-001",
                2
        );

        assertEquals(OrderStatus.PAID, order.getStatus());

        Inventory inventory = inventoryRepository
                .findByProductId("PROD-001")
                .orElseThrow();

        assertEquals(8, inventory.getQuantity());

        Payment payment = paymentRepository
                .findByOrderId(order.getId())
                .orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals("PROD-001", payment.getProductId());
        assertEquals(2, payment.getQuantity());
    }

    @Test
    void successfulOrderShouldCreateTransactionalOutboxEvent() {

        Order order = orderService.createOrder(
                "outbox-test-key",
                "PROD-001",
                2
        );

        List<OutboxEvent> events = outboxEventRepository.findAll();
        List<OutboxEvent> orderEvents = events.stream()
                .filter(e -> order.getId().equals(e.getAggregateId()))
                .toList();

        assertEquals(1, orderEvents.size());
        OutboxEvent event = orderEvents.get(0);
        assertEquals("ORDER_CREATED", event.getEventType());
        assertEquals(order.getId(), event.getAggregateId());
        assertNotNull(event.getStatus());
        assertTrue(
                event.getStatus() == OutboxStatus.PENDING ||
                event.getStatus() == OutboxStatus.PROCESSED
        );
    }

    @Test
    void kafkaConsumerShouldProcessDuplicateEventsOnlyOnce() {

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String payload = "{\"orderId\":1001,\"productId\":\"PROD-001\",\"quantity\":2,\"status\":\"PAID\"}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "order-events",
                0,
                0L,
                eventId.toString(),
                payload
        );

        orderEventConsumer.consume(record);
        orderEventConsumer.consume(record);

        assertTrue(processedEventRepository.existsByEventId(eventId));
        long count = processedEventRepository.findAll().stream()
                .filter(pe -> eventId.equals(pe.getEventId()))
                .count();
        assertEquals(1, count);
    }

    @Test
    void paymentCircuitBreakerShouldOpenAfterRepeatedFailures() {

        externalPaymentClient.setSimulateFailure(true);

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry.circuitBreaker("paymentCircuitBreaker");

        for (int i = 1; i <= 3; i++) {
            orderService.createOrder(
                    "circuit-breaker-test-00" + i,
                    "PROD-001",
                    1
            );
        }

        assertEquals(
                CircuitBreaker.State.OPEN,
                circuitBreaker.getState()
        );
    }

    @Test
    void paymentTimeoutShouldReleaseInventoryAndFailOrder() {

        externalPaymentClient.setSimulateTimeout(true);

        Order order = orderService.createOrder(
                "payment-timeout-test",
                "PROD-001",
                2
        );

        assertEquals(OrderStatus.FAILED, order.getStatus());

        Inventory inventory = inventoryRepository
                .findByProductId("PROD-001")
                .orElseThrow();

        assertEquals(10, inventory.getQuantity());
    }
}