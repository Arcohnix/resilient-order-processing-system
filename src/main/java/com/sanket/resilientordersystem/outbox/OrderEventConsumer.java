package com.sanket.resilientordersystem.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(
            ProcessedEventRepository processedEventRepository,
            ObjectMapper objectMapper
    ) {
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(
            topics = "order-events",
            groupId = "order-processing-group"
    )
    public void consume(ConsumerRecord<String, String> record) {

        try {

            UUID eventId = UUID.fromString(record.key());

            JsonNode event =
                    objectMapper.readTree(record.value());

            Long orderId = event
                    .get("orderId")
                    .asLong();

            if (processedEventRepository.existsByEventId(eventId)) {

                System.out.println(
                        ">>> DUPLICATE KAFKA EVENT IGNORED"
                                + " | orderId=" + orderId
                                + " | eventId=" + eventId
                );

                return;
            }

            ProcessedEvent processedEvent =
                    new ProcessedEvent(
                            eventId,
                            "ORDER_CREATED"
                    );

            processedEventRepository.save(processedEvent);

            System.out.println(
                    ">>> KAFKA EVENT PROCESSED"
                            + " | orderId=" + orderId
                            + " | eventId=" + eventId
            );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to process Kafka order event",
                    exception
            );
        }
    }
}