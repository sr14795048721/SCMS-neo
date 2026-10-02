package com.scms.core.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "scms.events", name = "mode", havingValue = "kafka")
public class KafkaDomainEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaDomainEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final DomainEventDispatcher dispatcher;

    public KafkaDomainEventConsumer(ObjectMapper objectMapper, DomainEventDispatcher dispatcher) {
        this.objectMapper = objectMapper;
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = {
            DomainEventTopics.ACTIVITY_PUBLISHED,
            DomainEventTopics.REGISTRATION_CREATED,
            DomainEventTopics.REGISTRATION_CANCELED,
            DomainEventTopics.NOTIFICATION_DISPATCH,
            DomainEventTopics.AUDIT_CREATED
    })
    public void onMessage(String payload) {
        try {
            DomainEvent event = objectMapper.readValue(payload, DomainEvent.class);
            dispatcher.dispatch(event);
        } catch (JsonProcessingException exception) {
            log.error("Invalid event payload: {}", payload, exception);
        }
    }
}
