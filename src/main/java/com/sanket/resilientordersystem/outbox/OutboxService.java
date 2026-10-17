package com.sanket.resilientordersystem.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanket.resilientordersystem.order.Order;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void createOrderCreatedEvent(Order order) {

        try {

            String payload = objectMapper.writeValueAsString(
                    new OrderCreatedEvent(
                            order.getId(),
                            order.getProductId(),
                            order.getQuantity(),
                            order.getStatus().name()
                    )
            );

            OutboxEvent event = new OutboxEvent(
                    "ORDER_CREATED",
                    order.getId(),
                    payload
            );

            outboxEventRepository.save(event);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to create order outbox event",
                    exception
            );
        }
    }

    private record OrderCreatedEvent(
            Long orderId,
            String productId,
            Integer quantity,
            String status
    ) {
    }
}