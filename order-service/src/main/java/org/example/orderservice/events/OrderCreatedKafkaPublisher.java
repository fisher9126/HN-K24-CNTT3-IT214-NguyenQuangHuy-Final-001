package org.example.orderservice.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCreatedKafkaPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OrderCreatedKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${notification.kafka.order-created-topic:order-created}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(topic, event.customerEmail());
    }
}
