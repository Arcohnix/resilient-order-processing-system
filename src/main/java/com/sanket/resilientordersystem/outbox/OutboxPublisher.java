package com.sanket.resilientordersystem.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final String TOPIC = "order-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING
                        );

        for (OutboxEvent event : events) {

            kafkaTemplate.send(
                    TOPIC,
                    event.getEventId().toString(),
                    event.getPayload()
            ).whenComplete((result, exception) -> {

                if (exception != null) {

                    System.err.println(
                            ">>> FAILED TO PUBLISH OUTBOX EVENT"
                                    + " | eventId="
                                    + event.getEventId()
                                    + " | error="
                                    + exception.getMessage()
                    );

                    return;
                }

                event.markProcessed();

                outboxEventRepository.save(event);

                System.out.println(
                        ">>> PUBLISHED OUTBOX EVENT"
                                + " | eventId="
                                + event.getEventId()
                                + " | topic="
                                + TOPIC
                );
            });
        }
    }
}