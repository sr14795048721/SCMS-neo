package com.scms.core.event;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "scms.events", name = "mode", havingValue = "sync", matchIfMissing = true)
public class SyncDomainEventPublisher implements DomainEventPublisher {

    private final DomainEventDispatcher dispatcher;

    public SyncDomainEventPublisher(DomainEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void publish(DomainEvent event) {
        dispatcher.dispatch(event);
    }
}
